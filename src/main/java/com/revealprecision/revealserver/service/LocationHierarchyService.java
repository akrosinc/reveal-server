package com.revealprecision.revealserver.service;

import static java.util.stream.Collectors.joining;

import com.revealprecision.revealserver.api.v1.dto.request.LocationHierarchyRequest;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTree;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.constant.Error;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.repository.LocationHierarchyRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
public class LocationHierarchyService {

  private LocationHierarchyRepository locationHierarchyRepository;
  private LocationRelationshipService locationRelationshipService;
  private JobScheduler jobScheduler;
  private GeographicLevelService geographicLevelService;

  @Autowired
  public LocationHierarchyService(LocationHierarchyRepository locationHierarchyRepository,
      LocationRelationshipService locationRelationshipService,
      JobScheduler jobScheduler,
      GeographicLevelService geographicLevelService) {
    this.locationHierarchyRepository = locationHierarchyRepository;
    this.locationRelationshipService = locationRelationshipService;
    this.jobScheduler = jobScheduler;
    this.geographicLevelService = geographicLevelService;
  }

  public LocationHierarchy createLocationHierarchy(
      LocationHierarchyRequest locationHierarchyRequest) {

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


  public List<LocationHierarchy> findByNodeOrder(List<String> nodeOrder) {
    return locationHierarchyRepository
        .findByNodeOrderArray(nodeOrder.stream().collect(joining(",", "{", "}")));
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

  public GeoTree getGeoTreeFromLocationHierarchy(LocationHierarchy locationHierarchy) {
    Optional<List<LocationRelationship>> locationRelationshipOptional = getLocationRelationshipsForLocationHierarchy(
        locationHierarchy);
    GeoTree geoTree = new GeoTree();
    geoTree.buildTreeFromList(
        locationRelationshipOptional.isPresent() ? locationRelationshipOptional.get() :
            Collections.emptyList());
    return geoTree;
  }

  public GeoTree getGeoTreeFromLocationHierarchyWithoutStructure(LocationHierarchy locationHierarchy) {
    Optional<List<LocationRelationship>> locationRelationshipOptional = locationRelationshipService.getLocationRelationshipsWithoutStructure(locationHierarchy);
    GeoTree geoTree = new GeoTree();
    geoTree.buildTreeFromList(
        locationRelationshipOptional.isPresent() ? locationRelationshipOptional.get() :
            Collections.emptyList());
    return geoTree;
  }

  public Optional<List<LocationRelationship>> getLocationRelationshipsForLocationHierarchy(
      LocationHierarchy locationHierarchy) {
    return locationRelationshipService
        .getLocationRelationshipsForLocationHierarchy(locationHierarchy);
  }

  public List<LocationHierarchy> findByName(String name) {
    return locationHierarchyRepository.findByName(name);
  }
}
