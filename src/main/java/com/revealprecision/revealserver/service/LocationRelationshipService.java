package com.revealprecision.revealserver.service;

import static com.revealprecision.revealserver.constants.LocationConstants.STRUCTURE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.MultiPolygon;
import com.mapbox.geojson.gson.GeometryGeoJson;
import com.mapbox.turf.TurfJoins;
import com.mapbox.turf.TurfMeasurement;
import com.revealprecision.revealserver.enums.BulkStatusEnum;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.messaging.message.LocationRelationshipMessage;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationBulk;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.projection.LocationChildrenCountProjection;
import com.revealprecision.revealserver.persistence.projection.LocationMainData;
import com.revealprecision.revealserver.persistence.projection.LocationRelationshipProjection;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import com.revealprecision.revealserver.persistence.repository.GeographicLevelRepository;
import com.revealprecision.revealserver.persistence.repository.LocationBulkRepository;
import com.revealprecision.revealserver.persistence.repository.LocationHierarchyRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRelationshipRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import com.revealprecision.revealserver.props.KafkaProperties;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.util.Pair;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LocationRelationshipService {

  private final LocationRelationshipRepository locationRelationshipRepository;
  private final GeographicLevelRepository geographicLevelRepository;
  private final LocationRepository locationRepository;
  private final LocationHierarchyRepository locationHierarchyRepository;
  private final RestHighLevelClient client;
  private final KafkaTemplate<String, LocationRelationshipMessage> kafkaTemplate;
  private final KafkaProperties kafkaProperties;
  private final LocationBulkRepository locationBulkRepository;
  private final Logger importLog = LoggerFactory.getLogger("location-import-file");
  private final Environment env;

  @Autowired
  public LocationRelationshipService(LocationRelationshipRepository locationRelationshipRepository,
      GeographicLevelRepository geographicLevelRepository, LocationRepository locationRepository,
      LocationHierarchyRepository locationHierarchyRepository, RestHighLevelClient client,
      KafkaTemplate<String, LocationRelationshipMessage> kafkaTemplate,
      KafkaProperties kafkaProperties, LocationBulkRepository locationBulkRepository,
      Environment env) {
    this.locationRelationshipRepository = locationRelationshipRepository;
    this.geographicLevelRepository = geographicLevelRepository;
    this.locationRepository = locationRepository;
    this.locationHierarchyRepository = locationHierarchyRepository;
    this.client = client;
    this.kafkaTemplate = kafkaTemplate;
    this.kafkaProperties = kafkaProperties;
    this.locationBulkRepository = locationBulkRepository;
    this.env = env;
  }


  @Async
  public void createLocationRelationships(LocationHierarchy locationHierarchy) {

    List<String> nodes = locationHierarchy.getNodeOrder();
    List<GeographicLevel> geographicLevels = nodes.stream()
        .map(node -> geographicLevelRepository.findByName(node).get()).collect(Collectors.toList());
    Map<GeographicLevel, List<Location>> geographicLevelToLocationsMap = new LinkedHashMap<>();

    geographicLevels.forEach(geographicLevel -> {
      List<Location> locations = locationRepository
          .findByGeographicLevelIdentifier(geographicLevel.getIdentifier());
      geographicLevelToLocationsMap.put(geographicLevel, locations);
    });

    geographicLevelToLocationsMap.get(geographicLevels.get(0))
        .forEach(location -> createRootLocationRelationship(location, locationHierarchy));

    geographicLevelToLocationsMap.entrySet().stream().forEach(item -> {
      List<Location> parentLocations = item.getValue();
      if (geographicLevels.size() > geographicLevels.indexOf(item.getKey()) + 1) {
        List<Location> potentialChildren = geographicLevelToLocationsMap
            .get(geographicLevels.get(geographicLevels.indexOf(item.getKey()) + 1));
        for (Location location : parentLocations) {
          for (Location potentialChild : potentialChildren) {
            try {
              createParentChildRelationship(location, potentialChild, locationHierarchy);
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      }
    });

  }

  public void deleteLocationRelationshipsForHierarchy(LocationHierarchy locationHierarchy) {
    List<LocationRelationship> locationRelationshipsToDelete = locationHierarchy
        .getLocationRelationships();
    if (!locationRelationshipsToDelete.isEmpty()) {
      locationRelationshipRepository.deleteAllById(
          locationRelationshipsToDelete.stream().map(LocationRelationship::getIdentifier).collect(
              Collectors.toList()));
    }
  }

  public void createRootLocationRelationship(Location location,
      LocationHierarchy locationHierarchy) {
    var locationRelationshipToSave = LocationRelationship.builder().location(location)
        .locationHierarchy(locationHierarchy).ancestry(
            Collections.emptyList())
        .build();
    locationRelationshipToSave.setEntityStatus(EntityStatus.ACTIVE);
    locationRelationshipRepository.save(locationRelationshipToSave);
  }

  public void updateLocationRelationshipsForNewLocation(Location location) throws IOException {
    var locationHierarchies = locationHierarchyRepository
        .findLocationHierarchiesByNodeOrderContaining(location.getGeographicLevel().getName());
    for (var locationHierarchy : locationHierarchies) {

      Integer nodePosition =
          locationHierarchy.getNodeOrder().indexOf(location.getGeographicLevel().getName()) - 1;
      if (nodePosition < locationHierarchy.getNodeOrder().size() && nodePosition >= 0) {
        var parentGeographicLevelName = locationHierarchy.getNodeOrder()
            .get(nodePosition);

        var parentGeographicLevel = geographicLevelRepository.findByName(parentGeographicLevelName);

        var upperGeographicLevelLocations = locationRepository
            .findByGeographicLevelIdentifier(parentGeographicLevel.get()
                .getIdentifier());

        for (Location parentLocation : upperGeographicLevelLocations) {
          createParentChildRelationship(parentLocation, location,
              locationHierarchy);
        }
      } else if (nodePosition == -1) {
        createRelationshipForRoot(location, locationHierarchy);
      }
    }
  }

  private void
  createParentChildRelationship(Location parentLocation, Location childLocation,
      LocationHierarchy locationHierarchy) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String parentGeometry = null;
    String childGeometry = null;
    try {
      parentGeometry = mapper.writeValueAsString(parentLocation.getGeometry());
      childGeometry = mapper.writeValueAsString(childLocation.getGeometry());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }

    Geometry mbChildGeometry = GeometryGeoJson.fromJson(childGeometry);
    Feature centerFeature = TurfMeasurement.center(Feature.fromGeometry(mbChildGeometry));
    com.mapbox.geojson.Point centerPoint = com.mapbox.geojson.Point.fromJson(
        centerFeature.geometry().toJson());
    Geometry mbParentGeometry = GeometryGeoJson.fromJson(parentGeometry);
    MultiPolygon parentPolygon = MultiPolygon.fromJson(mbParentGeometry.toJson());
    if (TurfJoins.inside(centerPoint, parentPolygon)) {
      List<UUID> ancestry = getAncestryFromParentLocation(parentLocation, locationHierarchy);
      LocationRelationship locationRelationshipToSave = LocationRelationship.builder()
          .parentLocation(parentLocation)
          .location(childLocation)
          .locationHierarchy(locationHierarchy)
          .ancestry(ancestry)
          .build();
      locationRelationshipToSave.setEntityStatus(EntityStatus.ACTIVE);
      locationRelationshipRepository.save(locationRelationshipToSave);
    }
  }

  private void createRelationshipForRoot(Location location, LocationHierarchy locationHierarchy) {
    LocationRelationship locationRelationship = LocationRelationship.builder()
        .location(location)
        .locationHierarchy(locationHierarchy)
        .build();
    locationRelationship.setEntityStatus(EntityStatus.ACTIVE);
    locationRelationshipRepository.save(locationRelationship);
  }


  private List<UUID> getAncestryFromParentLocation(Location parentLocation,
      LocationHierarchy locationHierarchy) {
    List<UUID> ancestry = new ArrayList<>();
    if (parentLocation == null) {
      return ancestry;
    }
    Optional<LocationRelationship> locationRelationshipOptional = locationRelationshipRepository
        .findByLocationHierarchyAndLocation(
            locationHierarchy, parentLocation);
    if (locationRelationshipOptional.isPresent()) {
      ancestry.addAll(locationRelationshipOptional.get().getAncestry());
    }
    ancestry.add(parentLocation.getIdentifier());
    return ancestry;
  }

  public Long getNumberOfChildrenByGeoLevelNameWithinLocationAndHierarchy(
      String geographicLevelName
      , UUID locationIdentifier,
      UUID locationHierarchyIdentifier) {

    return locationRelationshipRepository.getNumberOfChildrenByGeoLevelNameWithinLocationAndHierarchy(
        geographicLevelName
        , locationIdentifier.toString(), locationHierarchyIdentifier);

  }

  public List<LocationRelationship> getLocationRelationshipsForLocationHierarchy(
      LocationHierarchy locationHierarchy) {
    return locationRelationshipRepository.findByLocationHierarchyIdentifier(
        locationHierarchy.getIdentifier()).stream().map((LocationRelationship::new)).collect(
        Collectors.toList());
  }

  public List<LocationRelationshipProjection> getLocationRelationshipsNotLike(
      LocationHierarchy locationHierarchy, List<String> notLike) {
    return locationRelationshipRepository.findByLocationHierarchyWithoutStructuresNotLike(notLike);
  }

  public List<LocationRelationshipProjection> getLocationRelationshipsWithoutStructure(
      LocationHierarchy locationHierarchy) {
    return locationRelationshipRepository.findByLocationHierarchyWithoutStructures(
        locationHierarchy.getIdentifier());
  }

  public List<LocationChildrenCountProjection> getLocationChildrenCount(
      UUID locationHierarchyIdentifier) {
    return locationRelationshipRepository.getLocationChildrenCount(locationHierarchyIdentifier);
  }

  public List<LocationChildrenCountProjection> getLocationAssignedChildrenCount(
      UUID locationHierarchyIdentifier, UUID planIdentifier) {
    return locationRelationshipRepository.getLocationAssignedChildrenCount(
        locationHierarchyIdentifier, planIdentifier);
  }

  public long getNumberOfChildren(UUID locationIdentifier) {
    Optional<Long> response = locationRelationshipRepository.getChildrenNumber(locationIdentifier);
    if (response.isPresent()) {
      return response.get();
    }
    return 0;
  }

  public List<Location> getLocationChildrenByLocationParentIdentifierAndHierarchyIdentifier(
      List<UUID> parentLocationIdentifiers, UUID hierarchyIdentifier) {
    return locationRelationshipRepository.findLocationRelationshipUuidsByParentLocation_IdentifierAndHierarchyIdentifier(
        parentLocationIdentifiers, hierarchyIdentifier);
  }

  public List<PlanLocationDetails> getLocationChildrenByLocationParentIdentifierAndPlanIdentifier(
      UUID parentLocationIdentifiers, UUID planIdentifier) {

    return locationRelationshipRepository.getLocationDetailsByParentIdAndPlanId(
        parentLocationIdentifiers, planIdentifier);
  }

  public List<PlanLocationDetails> getAssignedLocationChildrenByLocationParentIdentifierAndPlanIdentifier(
      UUID parentLocationIdentifiers, UUID planIdentifier) {
    return locationRelationshipRepository.getAssignedLocationDetailsByParentIdAndPlanId(
        parentLocationIdentifiers, planIdentifier);
  }

  public PlanLocationDetails getRootLocationDetailsByPlanId(UUID planIdentifier) {
    return locationRelationshipRepository.getRootLocationDetailsByAndPlanId(planIdentifier);
  }


  public Location findParentLocationByLocationIdAndHierarchyId(UUID locationIdentifier,
      UUID hierarchyIdentifier) {
    return locationRelationshipRepository.getParentLocationByLocationIdAndHierarchyId(
        locationIdentifier, hierarchyIdentifier);
  }

  @Async("getAsyncExecutor")
  public void createRelationshipForImportedLocation(Location location, int index,
      int locationListSize) throws IOException {
    updateLocationRelationshipsForNewLocation(location);
    if (index == locationListSize - 1) {
      LocationBulk locationBulk = location.getLocationBulk();
      locationBulk.setStatus(BulkStatusEnum.COMPLETE);
      locationBulkRepository.save(locationBulk);
    }
  }

  public LocationRelationship getLocationRelationshipsForLocation(
      UUID locationHierarchyIdentifier, UUID locationIdentifier) {
    return locationRelationshipRepository.getLocationRelationshipByLocation_IdentifierAndLocationHierarchy_Identifier(
        locationIdentifier, locationHierarchyIdentifier);
  }


  public List<Location> getChildrenLocations(UUID hierarchyIdentifier, UUID locationIdentifier) {
    List<Location> children = locationRelationshipRepository.getChildren(hierarchyIdentifier,
        locationIdentifier);
    return children;
  }


  public Set<Location> getStructuresForPlanIfHierarchyHasStructure(
      LocationHierarchy locationHierarchy, List<Pair<UUID, String>> planLocations) {

    // 1. check if hierarchy has structures
    // 2. get the parent of structure from the hierarchy
    // 3. get children locations of the locations that has that parent node name
    // 4. If structure is not in hierarchy then get children of the lowest location level in hierarchy

    Set<Location> structureLocations = new HashSet<>();

    if (locationHierarchy.getNodeOrder().contains(STRUCTURE)) {
      Set<Location> structures = planLocations.stream()
          .filter(planLocation -> planLocation.getSecond().equals(
              locationHierarchy.getNodeOrder()
                  .get(locationHierarchy.getNodeOrder().indexOf(STRUCTURE) - 1))
          )
          .flatMap(filteredPlanLocation -> getChildrenLocations(
              locationHierarchy.getIdentifier(), filteredPlanLocation.getFirst())
              .stream()).collect(Collectors.toSet());
      structureLocations.addAll(structures);
    } else {
      Set<Location> structures = planLocations.stream()
          .filter(planLocation -> planLocation.getSecond().equals(
              locationHierarchy.getNodeOrder()
                  .get(locationHierarchy.getNodeOrder().size() - 1))
          ).flatMap(filteredPlanLocation -> getChildrenLocations(
              locationHierarchy.getIdentifier(), filteredPlanLocation.getFirst())
              .stream()).collect(Collectors.toSet());
      structureLocations.addAll(structures);
    }

    return structureLocations;
  }

  public Location getLocationParent(Location location, LocationHierarchy locationHierarchy) {
    return locationRelationshipRepository.getParentLocationByLocationIdAndHierarchyId(
        location.getIdentifier(), locationHierarchy.getIdentifier());
  }

  public List<LocationMainData> getLocationsByHierarchyIdAndLevelName(UUID hierarchyIdentifier,
      String levelName) {
    return locationRelationshipRepository.getLocationsByHierarchyIdAndLevelName(hierarchyIdentifier,
        levelName);
  }

  public Location getLocationParentByLocationIdentifierAndHierarchyIdentifier(
      UUID locationIdentifier, UUID locationHierarchyIdentifier) {
    return locationRelationshipRepository.getParentLocationByLocationIdAndHierarchyId(
        locationIdentifier, locationHierarchyIdentifier);
  }

  public List<UUID> getAncestryForLocation(UUID locationId, UUID locationHierarchyId) {
    return locationRelationshipRepository.getLocationRelationshipByLocation_IdentifierAndLocationHierarchy_Identifier(
        locationId, locationHierarchyId).getAncestry();
  }

}

