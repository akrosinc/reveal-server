package com.revealprecision.revealserver.service;

import static java.util.stream.Collectors.joining;

import com.revealprecision.revealserver.api.v1.dto.request.LocationHierarchyRequest;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationPropertyResponse;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.NotImplementedException;
import com.revealprecision.revealserver.exceptions.constant.Error;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.domain.Setting.Fields;
import com.revealprecision.revealserver.persistence.projection.LocationChildrenCountProjection;
import com.revealprecision.revealserver.persistence.projection.LocationRelationshipProjection;
import com.revealprecision.revealserver.persistence.repository.LocationHierarchyRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.NotSupportedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LocationHierarchyService {

  private final LocationHierarchyRepository locationHierarchyRepository;
  private final LocationRelationshipService locationRelationshipService;
  private final GeographicLevelService geographicLevelService;


  public LocationHierarchy createLocationHierarchy(
      LocationHierarchyRequest locationHierarchyRequest) {
    enforceOneHierarchyPerInstance();
    geographicLevelService.validateGeographyLevels(locationHierarchyRequest.getNodeOrder());
    validateLocationHierarchy(locationHierarchyRequest);

    var locationHierarchyToSave = LocationHierarchy.builder()
        .nodeOrder(locationHierarchyRequest.getNodeOrder()).name(locationHierarchyRequest.getName())
        .build();
    locationHierarchyToSave.setEntityStatus(EntityStatus.ACTIVE);
    var savedLocationHierarchy = locationHierarchyRepository.save(locationHierarchyToSave);
    locationRelationshipService.createLocationRelationships(savedLocationHierarchy);
    return savedLocationHierarchy;
  }

  private void enforceOneHierarchyPerInstance() {
    if (locationHierarchyRepository.activeHierarchyCount() > 0) {
      throw new NotImplementedException(Error.ONE_HIERARCHY_SUPPORT);
    }
  }

  private void validateLocationHierarchy(LocationHierarchyRequest locationHierarchyRequest) {
    List<LocationHierarchy> existingHierarchy = findByNodeOrder(
        locationHierarchyRequest.getNodeOrder());
    if (existingHierarchy != null && !existingHierarchy.isEmpty()) {
      throw new ConflictException(
          String.format(Error.NON_UNIQUE, LocationHierarchy.Fields.nodeOrder,
              locationHierarchyRequest.getNodeOrder()));
    }
  }

  public Page<LocationHierarchy> getLocationHierarchies(Pageable pageable) {
    return locationHierarchyRepository.findAll(pageable);
  }

  public Set<LocationHierarchy> getLocationHierarchiesIn(Set<UUID> locationHierarchyIdentifiers) {
    return locationHierarchyRepository.findLocationHierarchiesByIdentifierIn(
        locationHierarchyIdentifiers);
  }

  public List<LocationHierarchy> findByNodeOrder(List<String> nodeOrder) {
    return locationHierarchyRepository
        .findByNodeOrderArray(nodeOrder.stream().collect(joining(",", "{", "}")));
  }

  public List<LocationHierarchy> getAll() {
    return locationHierarchyRepository
        .findAll();
  }

  public void deleteLocationHierarchyAndAssociatedLocationRelationships(UUID identifier) {
    LocationHierarchy locationHierarchy = findByIdentifier(identifier);
    locationRelationshipService.deleteLocationRelationshipsForHierarchy(locationHierarchy);
    deleteLocationHierarchy(locationHierarchy);
  }

  private void deleteLocationHierarchy(LocationHierarchy locationHierarchy) {
    locationHierarchyRepository.delete(locationHierarchy);
  }

  public LocationHierarchy findByIdentifier(UUID identifier) {
    return locationHierarchyRepository.findById(identifier).orElseThrow(
        () -> new NotFoundException(Pair.of(LocationHierarchy.Fields.identifier, identifier),
            LocationHierarchy.class));
  }

  public UUID findNativeByName(String hierarchyName) {
    return locationHierarchyRepository.findLocationHierarchyByName(hierarchyName);
  }

  public UUID findNativeById(UUID hierarchyIdentifier) {
    return locationHierarchyRepository.findLocationHierarchyByIdentifier(hierarchyIdentifier);
  }

  public List<GeoTreeResponse> getGeoTreeFromLocationHierarchy(
      LocationHierarchy locationHierarchy) {
    List<LocationRelationship> locationRelationship = getLocationRelationshipsForLocationHierarchy(
        locationHierarchy);
    List<GeoTreeResponse> geoTreeResponses = locationRelationship.stream()
        .map(lr -> GeoTreeResponse.builder()
            .identifier(lr.getLocation().getIdentifier())
            .properties(LocationPropertyResponse.builder()
                .parentIdentifier((lr.getParentLocation() == null) ? UUID.fromString(
                    "00000000-0000-0000-0000-000000000000")
                    : lr.getParentLocation().getIdentifier())
                .name(lr.getLocation().getName())
                .geographicLevel(lr.getLocation().getGeographicLevel().getName())
                .build())
            .build()).collect(Collectors.toList());
    Map<UUID, List<GeoTreeResponse>> geoTreeHierarchy = geoTreeResponses.stream()
        .collect(Collectors.groupingBy(lr -> lr.getProperties().getParentIdentifier(),
            Collectors.mapping(lr -> lr, Collectors.toList())));

    geoTreeResponses.forEach(gt -> gt.setChildren(
        geoTreeHierarchy.get(gt.getIdentifier()) == null ? new ArrayList<>()
            : geoTreeHierarchy.get(gt.getIdentifier())));
    return geoTreeHierarchy.get(UUID.fromString("00000000-0000-0000-0000-000000000000"));
  }

  public List<GeoTreeResponse> getGeoTreeFromLocationHierarchyWithoutStructure(
      LocationHierarchy locationHierarchy, List<String> notLike) {
    List<LocationRelationshipProjection> locationRelationship =
        notLike != null ? locationRelationshipService.getLocationRelationshipsNotLike(
            locationHierarchy, notLike)
            : locationRelationshipService.getLocationRelationshipsWithoutStructure(
                locationHierarchy);
    Map<String, Long> childrenCount = locationRelationshipService.getLocationChildrenCount(
            locationHierarchy.getIdentifier())
        .stream().filter(loc -> loc.getParentIdentifier() != null)
        .collect(Collectors.toMap(LocationChildrenCountProjection::getParentIdentifier,
            LocationChildrenCountProjection::getChildrenCount));

    List<GeoTreeResponse> geoTreeResponses = locationRelationship.stream()
        .map(lr -> GeoTreeResponse.builder()
            .identifier(UUID.fromString(lr.getLocationIdentifier()))
            .properties(LocationPropertyResponse.builder()
                .parentIdentifier((lr.getParentIdentifier() == null) ? UUID.fromString(
                    "00000000-0000-0000-0000-000000000000")
                    : UUID.fromString(lr.getParentIdentifier()))
                .name(lr.getLocationName())
                .geographicLevel(lr.getGeographicLevelName())
                .childrenNumber(
                    childrenCount.containsKey(lr.getLocationIdentifier()) ? childrenCount.get(
                        lr.getLocationIdentifier()) : 0)
                .build())
            .build()).collect(Collectors.toList());
    Map<UUID, List<GeoTreeResponse>> geoTreeHierarchy = geoTreeResponses.stream()
        .collect(Collectors.groupingBy(lr -> lr.getProperties().getParentIdentifier(),
            Collectors.mapping(lr -> lr, Collectors.toList())));

    geoTreeResponses.forEach(gt -> gt.setChildren(
        geoTreeHierarchy.get(gt.getIdentifier()) == null ? new ArrayList<>()
            : geoTreeHierarchy.get(gt.getIdentifier())));
    return geoTreeHierarchy.get(UUID.fromString("00000000-0000-0000-0000-000000000000"));
  }

  public List<LocationRelationship> getLocationRelationshipsForLocationHierarchy(
      LocationHierarchy locationHierarchy) {
    return locationRelationshipService
        .getLocationRelationshipsForLocationHierarchy(locationHierarchy);
  }

  public List<LocationHierarchy> findByName(String name) {
    return locationHierarchyRepository.findByName(name);
  }

  public LocationHierarchy getActiveLocationHierarchy() {
    //Assumption: current support of 1 hierarchy per instance
    LocationHierarchy locationHierarchy = null;
    List<LocationHierarchy> hierarchies = locationHierarchyRepository.findAll();
    if (!hierarchies.isEmpty()) {
      locationHierarchy = hierarchies.get(0);
    }
    return locationHierarchy;
  }
}
