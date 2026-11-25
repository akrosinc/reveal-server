package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.InstanceCustomLocationRequest;
import com.revealprecision.revealserver.api.v1.dto.request.InstanceLocationEntityTagCountRequest;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceLocationEntityTagCountResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.config.InstanceContext;
import com.revealprecision.revealserver.constants.EntityTagDataTypes;
import com.revealprecision.revealserver.constants.EntityTagFieldTypes;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.LocationStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.InstanceCustomLocation;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationProperty;
import com.revealprecision.revealserver.persistence.domain.aggregation.ImportAggregationNumeric;
import com.revealprecision.revealserver.persistence.projection.LocationEntityTagCountProjection;
import com.revealprecision.revealserver.persistence.repository.ImportAggregationNumericRepository;
import com.revealprecision.revealserver.persistence.repository.InstanceCustomLocationRepository;
import com.revealprecision.revealserver.persistence.repository.InstanceRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import com.revealprecision.revealserver.util.JtsGeometryUtil;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
Instance custom locations drawn under a shared Health Facility, and points inside a custom location.
Always scoped to the instance of the current request (X-Instance-ID, see InstanceContextFilter).
*/
@Service
@RequiredArgsConstructor
@Slf4j
public class InstanceCustomLocationService {

  private final InstanceRepository instanceRepository;
  private final InstanceCustomLocationRepository instanceCustomLocationRepository;
  private final LocationRepository locationRepository;
  private final GeographicLevelService geographicLevelService;
  private final EntityTagService entityTagService;
  private final ImportAggregationNumericRepository importAggregationNumericRepository;
  private final MetadataService metadataService;

  /** Creates a custom location and its points in one transaction; any failure rolls everything back. */
  @Transactional
  public List<LocationResponse> createCustomLocation(InstanceCustomLocationRequest request) {
    Instance instance = findInstanceById(InstanceContext.get());
    InstanceCustomLocation newCustomLocation = InstanceCustomLocation.builder().instance(instance).build();
    InstanceCustomLocation builtCustomLocation = buildCustomLocation(newCustomLocation, request);
    InstanceCustomLocation customLocation = instanceCustomLocationRepository.save(builtCustomLocation);
    List<InstanceCustomLocation> points = savePoints(instance, customLocation, request);
    incrementParentLocationTag(instance, customLocation.getParentLocation(), request.getTagName());
    return toResponses(customLocation, points);
  }

  /**
   * Adds one to the request's tag for the custom location's parent location: a 'user-set' row (no
   * dataCaptureDate) in import_aggregation_numeric for the instance's hierarchy, created with 1 when
   * absent. Only the parent location itself is updated, not its ancestors.
   */
  private void incrementParentLocationTag(Instance instance, Location parentLocation,
      String tagName) {
    if (StringUtils.isEmpty(tagName)) {
      return;
    }
    EntityTag entityTag = entityTagService.getEntityTagByTagName(tagName)
        .orElseThrow(() -> new IllegalArgumentException("Entity tag not found"));

    addToParentLocationTag(instance, parentLocation, entityTag.getTag(), 1);
  }

  /**
   * Takes one off the tag a custom location incremented on its parent location when it was created
   * (see {@link #incrementParentLocationTag}); never goes below 0, and does nothing when there is no
   * such row.
   */
  private void decrementParentLocationTag(Instance instance, Location parentLocation,
      String tagName) {
    addToParentLocationTag(instance, parentLocation, tagName, -1);
  }

  /**
   * Adds delta to the parent location's 'user-set' row for the tag. A missing row is created only for
   * a positive delta, and the value never goes below 0.
   */
  private void addToParentLocationTag(Instance instance, Location parentLocation, String tag,
      double delta) {
    if (StringUtils.isEmpty(tag) || parentLocation == null) {
      return;
    }
    Optional<ImportAggregationNumeric> existing = findParentLocationTagAggregation(instance,
        parentLocation, tag);
    if (existing.isEmpty() && delta <= 0) {
      return;
    }
    ImportAggregationNumeric aggregation = existing.orElseGet(() -> ImportAggregationNumeric.builder()
        .name(parentLocation.getName())
        .ancestor(parentLocation.getIdentifier().toString())
        .hierarchyIdentifier(instance.getLocationHierarchy().getIdentifier().toString())
        .fieldCode(tag)
        .eventType(EntityTagFieldTypes.USER_SET)
        .planIdentifier(instance.getIdentifier().toString())
        .val(0d)
        .build());
    double val = aggregation.getVal() == null ? 0d : aggregation.getVal();
    aggregation.setVal(Math.max(0d, val + delta));
    importAggregationNumericRepository.save(aggregation);
  }

  private Optional<ImportAggregationNumeric> findParentLocationTagAggregation(Instance instance,
      Location parentLocation, String tag) {
    return importAggregationNumericRepository
        .findByNameAndAncestorAndFieldCodeAndHierarchyIdentifierAndDataCaptureDateAndEventTypeAndPlanIdentifier(
            parentLocation.getName(), parentLocation.getIdentifier().toString(), tag,
            instance.getLocationHierarchy().getIdentifier().toString(), null,
            EntityTagFieldTypes.USER_SET, instance.getIdentifier().toString());
  }

  /** Custom locations of the current instance under a shared parent, each followed by its points. */
  public List<LocationResponse> getCustomLocations(UUID parentLocationId) {
    UUID instanceId = InstanceContext.get();
    List<InstanceCustomLocation> customLocations = instanceCustomLocationRepository
        .findAllByInstance_IdentifierAndParentLocation_IdentifierOrderByName(instanceId,
            parentLocationId);
    if (customLocations.isEmpty()) {
      return new ArrayList<>();
    }
    Map<UUID, List<InstanceCustomLocation>> pointsByCustomLocation = instanceCustomLocationRepository
        .findAllByInstance_IdentifierAndParent_IdentifierIn(instanceId,
            customLocations.stream().map(InstanceCustomLocation::getIdentifier).collect(Collectors.toList()))
        .stream()
        .collect(Collectors.groupingBy(point -> point.getParent().getIdentifier()));

    List<LocationResponse> response = new ArrayList<>();
    customLocations.forEach(customLocation -> response.addAll(
        toResponses(customLocation, pointsByCustomLocation.getOrDefault(customLocation.getIdentifier(), new ArrayList<>()))));
    return response;
  }

  /**
   * Adds the instance's custom locations to a location tree: each custom location under its shared parent
   * (Health Facility) node, with its points as the custom location's children. Custom locations whose parent is not in
   * the tree (e.g. outside a user's filtered tree) are left out.
   */
  public void attachCustomLocationsToTree(UUID instanceId, List<GeoTreeResponse> tree) {
    if (instanceId == null || tree == null || tree.isEmpty()) {
      return;
    }
    Map<UUID, GeoTreeResponse> nodes = new HashMap<>();
    indexTree(tree, nodes);
    List<InstanceCustomLocation> customLocations = instanceCustomLocationRepository
        .findAllByInstance_IdentifierAndParentLocation_IdentifierInOrderByName(instanceId,
            nodes.keySet());
    if (customLocations.isEmpty()) {
      return;
    }
    Map<UUID, List<InstanceCustomLocation>> pointsByCustomLocation = instanceCustomLocationRepository
        .findAllByInstance_IdentifierAndParent_IdentifierIn(instanceId,
            customLocations.stream().map(InstanceCustomLocation::getIdentifier).collect(Collectors.toList()))
        .stream()
        .collect(Collectors.groupingBy(point -> point.getParent().getIdentifier()));

    customLocations.forEach(customLocation -> {
      GeoTreeResponse parent = nodes.get(customLocation.getParentLocation().getIdentifier());
      List<GeoTreeResponse> children = parent.getChildren() == null ? new ArrayList<>()
          : new ArrayList<>(parent.getChildren());
      children.add(LocationResponseFactory.instanceCustomLocationToGeoTree(customLocation,
          pointsByCustomLocation.getOrDefault(customLocation.getIdentifier(), new ArrayList<>())));
      parent.setChildren(children);
    });
  }

  private void indexTree(List<GeoTreeResponse> tree, Map<UUID, GeoTreeResponse> nodes) {
    if (tree == null) {
      return;
    }
    tree.forEach(node -> {
      nodes.put(node.getIdentifier(), node);
      indexTree(node.getChildren(), nodes);
    });
  }

  /**
   * Sets counts of numeric entity tags for a location, through the same import aggregation tables
   * as a dataset upload (MetadataService#updateDB): like an upload, the value is written for the
   * location and each of its ancestors (location_relationship ancestry), stored with the current
   * instance's hierarchy identifier, and replaces the previous value for the same location, tag and
   * dataCaptureDate.
   */
  public void setLocationEntityTagCounts(InstanceLocationEntityTagCountRequest request) {
    if (StringUtils.isEmpty(request.getTag())) {
      return;
    }
    Instance instance = findInstanceById(InstanceContext.get());
    Location location = locationRepository.findById(request.getLocationId())
        .orElseThrow(() -> new IllegalArgumentException("Location not found"));

    EntityTag entityTag = entityTagService.getEntityTagByTagName(request.getTag())
        .orElseThrow(() -> new IllegalArgumentException("Entity tag not found"));

    String hierarchyIdentifier = instance.getLocationHierarchy().getIdentifier().toString();

    metadataService.updateDB(
        location.getName(), location.getIdentifier().toString(), request.getCount(), entityTag.getTag(),
        entityTag.getValueType(), hierarchyIdentifier, request.getDataCaptureDate(),
        EntityTagFieldTypes.USER_SET, instance.getIdentifier().toString());
  }

  /**
   * Entity tag counts of a location, as set by {@link #setLocationEntityTagCounts}, for the current
   * instance's hierarchy and the given dataCaptureDate (null: the values set without a date). Every
   * requested tag is returned; count and total are null when nothing was set.
   */
  public InstanceLocationEntityTagCountResponse getLocationEntityTagCounts(UUID locationId,
      List<String> tags, LocalDate dataCaptureDate) {
    Instance instance = findInstanceById(InstanceContext.get());
    Location location = locationRepository.findById(locationId)
        .orElseThrow(() -> new NotFoundException(Pair.of("identifier", locationId), Location.class));
    List<String> tagNames = tags == null ? new ArrayList<>()
        : tags.stream().distinct().collect(Collectors.toList());

    Map<String, LocationEntityTagCountProjection> counts = new HashMap<>();
    if (!tagNames.isEmpty()) {
      String hierarchyIdentifier = instance.getLocationHierarchy().getIdentifier().toString();
      String ancestor = location.getIdentifier().toString();
      String planIdentifier = instance.getIdentifier().toString();
      (dataCaptureDate == null
          ? importAggregationNumericRepository.findTagCountsByLocationWithoutDataCaptureDate(
              ancestor, location.getName(), hierarchyIdentifier, tagNames, planIdentifier)
          : importAggregationNumericRepository.findTagCountsByLocationAndDataCaptureDate(
              ancestor, location.getName(), hierarchyIdentifier, tagNames, dataCaptureDate,
              planIdentifier))
          .forEach(row -> counts.put(row.getTag(), row));
    }

    return InstanceLocationEntityTagCountResponse.builder()
        .locationId(location.getIdentifier())
        .dataCaptureDate(dataCaptureDate)
        .tags(tagNames.stream().map(tag -> {
          LocationEntityTagCountProjection row = counts.get(tag);
          return InstanceLocationEntityTagCountResponse.TagCount.builder()
              .tag(tag)
              .count(row == null ? null : row.getOwnCount())
              .total(row == null ? null : row.getTotalCount())
              .build();
        }).collect(Collectors.toList()))
        .build();
  }

  /**
   * Updates a custom location of the current instance. When points are sent they replace the current points
   * of the custom location; a point subtype is always re-derived from the custom location.
   */
  @Transactional
  public List<LocationResponse> updateCustomLocation(UUID identifier, InstanceCustomLocationRequest request) {
    InstanceCustomLocation existingCustomLocation = findCustomLocation(identifier);
    Location previousParentLocation = existingCustomLocation.getParentLocation();
    InstanceCustomLocation builtCustomLocation = buildCustomLocation(existingCustomLocation, request);
    InstanceCustomLocation customLocation = instanceCustomLocationRepository.save(builtCustomLocation);
    moveParentLocationTag(customLocation, previousParentLocation, request.getTagName());
    List<InstanceCustomLocation> currentPoints = findPoints(customLocation);
    List<InstanceCustomLocation> points;
    if (request.getPoints() == null) {
      String pointSubtype = locationSubtype(request);
      GeographicLevel pointGeographicLevel = findGeographicLevel(request.getPointGeographicLevelName());
      currentPoints.forEach(point -> {
        point.getMetadata().setLocationSubtype(pointSubtype);
        point.setGeographicLevel(pointGeographicLevel);
      });
      points = instanceCustomLocationRepository.saveAll(currentPoints);
    } else {
      instanceCustomLocationRepository.deleteAll(currentPoints);
      points = savePoints(customLocation.getInstance(), customLocation, request);
    }
    return toResponses(customLocation, points);
  }

  /**
   * When a custom location's parent changes, moves the count for the request's tag from the previous
   * parent location to the new one.
   */
  private void moveParentLocationTag(InstanceCustomLocation customLocation,
      Location previousParentLocation, String tagName) {
    Location parentLocation = customLocation.getParentLocation();
    if (StringUtils.isEmpty(tagName) || previousParentLocation == null
        || parentLocation == null
        || previousParentLocation.getIdentifier().equals(parentLocation.getIdentifier())) {
      return;
    }
    addToParentLocationTag(customLocation.getInstance(), previousParentLocation, tagName, -1);
    addToParentLocationTag(customLocation.getInstance(), parentLocation, tagName, 1);
  }

  /**
   * Soft-deletes a custom location of the current instance with its points. When tagName is given,
   * takes one off that tag on the custom location's parent location.
   */
  @Transactional
  public void deleteCustomLocation(UUID identifier, String tagName) {
    InstanceCustomLocation customLocation = findCustomLocation(identifier);
    List<InstanceCustomLocation> points = findPoints(customLocation);
    instanceCustomLocationRepository.deleteAll(points);
    instanceCustomLocationRepository.delete(customLocation);
    decrementParentLocationTag(customLocation.getInstance(), customLocation.getParentLocation(),
        tagName);
  }

  private InstanceCustomLocation findCustomLocation(UUID identifier) {
    return instanceCustomLocationRepository
        .findByIdentifierAndInstance_Identifier(identifier, InstanceContext.get())
        .orElseThrow(() -> new NotFoundException(
            Pair.of(InstanceCustomLocation.Fields.identifier, identifier),
            InstanceCustomLocation.class));
  }

  private List<InstanceCustomLocation> findPoints(InstanceCustomLocation customLocation) {
    return instanceCustomLocationRepository.findAllByInstance_IdentifierAndParent_IdentifierIn(
        customLocation.getInstance().getIdentifier(), List.of(customLocation.getIdentifier()));
  }

  private List<LocationResponse> toResponses(InstanceCustomLocation customLocation,
      List<InstanceCustomLocation> points) {
    List<LocationResponse> response = new ArrayList<>();
    response.add(LocationResponseFactory.fromInstanceCustomLocation(customLocation));
    points.forEach(point -> response.add(LocationResponseFactory.fromInstanceCustomLocation(point)));
    return response;
  }

  private InstanceCustomLocation buildCustomLocation(InstanceCustomLocation customLocation,
      InstanceCustomLocationRequest request) {
    String locationSubtype = locationSubtype(request);
    Location parentLocation = locationRepository.findById(request.getParentLocationIdentifier()).orElseThrow(
        () -> new NotFoundException("Parent location not found")
    );

    GeographicLevel boundaryGeographicLevel = findGeographicLevel(request.getBoundaryGeographicLevelName());
    Geometry boundary = JtsGeometryUtil.withWgs84Srid(request.getLocationBoundary());
    LocationProperty metadata = buildMetadata(request.getName(), locationSubtype);
    customLocation.setName(request.getName());
    customLocation.setStatus(LocationStatus.ACTIVE);
    customLocation.setParentLocation(parentLocation);
    customLocation.setGeographicLevel(boundaryGeographicLevel);
    customLocation.setGeometry(boundary);
    customLocation.setMetadata(metadata);
    customLocation.setEntityStatus(EntityStatus.ACTIVE);
    return customLocation;
  }

  // a point always has the same subtype as its custom location
  private String locationSubtype(InstanceCustomLocationRequest request) {
    return request.getLocationSubtype() == null ? null : request.getLocationSubtype().name();
  }

  private List<InstanceCustomLocation> savePoints(Instance instance, InstanceCustomLocation customLocation,
      InstanceCustomLocationRequest request) {
    if (request.getPoints() == null || request.getPoints().isEmpty()) {
      return new ArrayList<>();
    }
    String pointSubtype = locationSubtype(request);
    GeographicLevel pointGeographicLevel = findGeographicLevel(request.getPointGeographicLevelName());
    List<InstanceCustomLocation> points = new ArrayList<>();
    for (int i = 0; i < request.getPoints().size(); i++) {
      Point point = request.getPoints().get(i);
      String name = customLocation.getName() + " point " + (i + 1);
      Geometry pointGeometry = JtsGeometryUtil.withWgs84Srid(point);
      LocationProperty pointMetadata = buildMetadata(name, pointSubtype);
      InstanceCustomLocation instanceCustomLocation = InstanceCustomLocation.builder()
          .instance(instance)
          .name(name)
          .status(LocationStatus.ACTIVE)
          .parent(customLocation)
          .geographicLevel(pointGeographicLevel)
          .geometry(pointGeometry)
          .metadata(pointMetadata)
          .build();
      instanceCustomLocation.setEntityStatus(EntityStatus.ACTIVE);
      points.add(instanceCustomLocation);
    }
    return instanceCustomLocationRepository.saveAll(points);
  }

  private GeographicLevel findGeographicLevel(String name) {
    return name == null || name.isBlank() ? null : geographicLevelService.findByName(name);
  }

  private LocationProperty buildMetadata(String name, String locationSubtype) {
    return LocationProperty.builder()
        .name(name)
        .status(LocationStatus.ACTIVE)
        .locationSubtype(locationSubtype)
        .build();
  }

  private Instance findInstanceById(UUID identifier) {
    return instanceRepository.findById(identifier)
        .orElseThrow(
            () -> new NotFoundException(Pair.of("identifier", identifier), Instance.class));
  }
}
