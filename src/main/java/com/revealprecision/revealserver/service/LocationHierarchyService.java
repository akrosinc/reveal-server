package com.revealprecision.revealserver.service;

import static java.util.stream.Collectors.joining;

import com.revealprecision.revealserver.api.v1.dto.request.LocationHierarchyRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.constant.Error;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel.Fields;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.repository.LocationHierarchyRepository;
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
    locationHierarchyRequest.getNodeOrder().stream().forEach(nodeName -> {
      if (!geographicLevelService.findByName(nodeName).isPresent()) {
        throw new NotFoundException(Pair.of(Fields.name, nodeName), GeographicLevel.class);
      }
    });

    List<LocationHierarchy> existingHierarchy = findByNodeOrder(
        locationHierarchyRequest.getNodeOrder());
    if (existingHierarchy != null && !existingHierarchy.isEmpty()) {
      throw new ConflictException(
          String.format(Error.NON_UNIQUE, LocationHierarchy.Fields.nodeOrder,
              locationHierarchyRequest.getNodeOrder()));
    }

    var locationHierarchyToSave = LocationHierarchy.builder()
        .nodeOrder(locationHierarchyRequest.getNodeOrder()).build();
    locationHierarchyToSave.setEntityStatus(EntityStatus.ACTIVE);
    var savedLocationHierarchy = locationHierarchyRepository.save(locationHierarchyToSave);
    if (savedLocationHierarchy != null) {
      jobScheduler.enqueue(
          () -> locationRelationshipService.createLocationRelationships(savedLocationHierarchy));
    }
    return savedLocationHierarchy;
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
    deleteLocationHierarchy(locationHierarchy);
    jobScheduler.enqueue(() -> locationRelationshipService
        .deleteLocationRelationshipsForHierarchy(locationHierarchy)
    );
  }

  private void deleteLocationHierarchy(LocationHierarchy locationHierarchy) {
    locationHierarchyRepository.delete(locationHierarchy);
  }

  public LocationHierarchy findByIdentifier(UUID identifier) {
    return locationHierarchyRepository.findById(identifier).orElseThrow(
        () -> new NotFoundException(Pair.of(LocationHierarchy.Fields.identifier, identifier),
            LocationHierarchy.class));
  }

  public List<LocationHierarchy> findHierarchiesContainingGeographicLevel(
      String geographicLevelName) {
    return locationHierarchyRepository
        .findLocationHierarchiesByNodeOrderContaining(geographicLevelName);
  }

  public Optional<List<LocationRelationship>> getLocationRelationshipsForLocationHierarchy(
      LocationHierarchy locationHierarchy) {
    return locationRelationshipService
        .getLocationRelationshipsForLocationHierarchy(locationHierarchy);
  }

}
