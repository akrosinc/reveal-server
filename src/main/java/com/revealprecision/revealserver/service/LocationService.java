package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import com.revealprecision.revealserver.persistence.projection.LocationChildrenCountProjection;
import com.revealprecision.revealserver.persistence.projection.LocationCoordinatesProjection;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService {

  private final LocationRepository locationRepository;
  private final GeographicLevelService geographicLevelService;
  private final LocationRelationshipService locationRelationshipService;
  private final PlanService planService;

  public Location createLocation(LocationRequest locationRequest) {
    GeographicLevel geographicLevel = geographicLevelService.findByName(
        locationRequest.getProperties().getGeographicLevel());
    var locationToSave = Location.builder().geographicLevel(geographicLevel)
        .type(locationRequest.getType()).geometry(locationRequest.getGeometry())
        .name(locationRequest.getProperties().getName())
        .status(locationRequest.getProperties().getStatus())
        .externalId(locationRequest.getProperties().getExternalId()).build();
    if (locationRequest.getProperties().getExternalId() != null) {
      locationToSave.setIdentifier(locationRequest.getProperties().getExternalId());
    }
    locationToSave.setEntityStatus(EntityStatus.ACTIVE);
    var savedLocation = locationRepository.save(locationToSave);
    locationRelationshipService.updateLocationRelationshipsForNewLocation(savedLocation);
    return savedLocation;
  }

  public Location findByIdentifier(UUID identifier) {
    return locationRepository.findById(identifier).orElseThrow(
        () -> new NotFoundException(Pair.of(Location.Fields.identifier, identifier),
            Location.class));
  }

  public Page<Location> getLocations(String search, Pageable pageable) {
    return locationRepository.findAlLByCriteria(search, pageable);
  }

  public long getAllCount(String search) {
    return locationRepository.findAllCountByCriteria(search);
  }

  public void deleteLocation(UUID identifier) {
    Location location = locationRepository.findById(identifier).orElseThrow(
        () -> new NotFoundException(Pair.of(Location.Fields.identifier, identifier),
            Location.class));
    locationRepository.delete(location);
  }

  public Location updateLocation(UUID identifier, LocationRequest locationRequest) {
    Location location = findByIdentifier(identifier);
    GeographicLevel geographicLevel = geographicLevelService.findByName(
        locationRequest.getProperties().getGeographicLevel());
    return locationRepository.save(location.update(locationRequest, geographicLevel));
  }

  public List<Location> getAllByIdentifiers(List<UUID> identifiers) {
    return locationRepository.getAllByIdentifiers(identifiers);
  }

  public List<Location> getAllByNames(List<String> names) {
    return locationRepository.getAllByNames(names);
  }

  public List<Location> getStructuresByParentIdentifiers(List<UUID> parentIdentifiers,
      LocationHierarchy locationHierarchy, Long serverVersion) {
    List<LocationRelationship> locationRelationships = locationHierarchy.getLocationRelationships();
    List<Location> locations = locationRelationships.stream().filter(
            locationRelationship -> locationRelationship.getParentLocation() != null
                && parentIdentifiers.contains(locationRelationship.getParentLocation().getIdentifier()))
        .map(LocationRelationship::getLocation).collect(Collectors.toList());

    return locations.stream()
        .filter(location -> location.getGeographicLevel().getName().equalsIgnoreCase(
            LocationConstants.STRUCTURE)
            && location.getServerVersion() >= serverVersion)
        .collect(Collectors.toList());//TODO: to update once we figure the target level part.
  }

  public List<PlanLocationDetails> getLocationsByParentIdentifierAndPlanIdentifier(
      UUID parentIdentifier,
      UUID planIdentifier) {
    Plan plan = planService.findPlanByIdentifier(planIdentifier);
    Location location = findByIdentifier(parentIdentifier);

    Map<UUID, Long> childrenCount = locationRelationshipService.getLocationChildrenCount(
            plan.getLocationHierarchy().getIdentifier())
        .stream().filter(loc -> loc.getParentIdentifier() != null)
        .collect(Collectors.toMap(loc -> UUID.fromString(loc.getParentIdentifier()),
            LocationChildrenCountProjection::getChildrenCount));
    List<PlanLocationDetails> response = locationRelationshipService
        .getLocationChildrenByLocationParentIdentifierAndPlanIdentifier(
            parentIdentifier, planIdentifier);
    response.forEach(resp -> resp.setChildrenNumber(
        childrenCount.containsKey(resp.getLocation().getIdentifier()) ? childrenCount.get(
            resp.getLocation().getIdentifier()) : 0));
    if (location != null) {
      response = response.stream()
          .peek(planLocationDetail -> planLocationDetail.setParentLocation(location))
          .collect(Collectors.toList());
    }
    return response;
  }

  public List<PlanLocationDetails> getAssignedLocationsByParentIdentifierAndPlanIdentifier(
      UUID parentIdentifier,
      UUID planIdentifier,
      boolean isBeforeStructure) {
    Plan plan = planService.findPlanByIdentifier(planIdentifier);
    Location location = findByIdentifier(parentIdentifier);
    Map<UUID, Long> childrenCount;
    if (!isBeforeStructure) {
      childrenCount = locationRelationshipService.getLocationAssignedChildrenCount(
              plan.getLocationHierarchy().getIdentifier(), planIdentifier)
          .stream().filter(loc -> loc.getParentIdentifier() != null)
          .collect(Collectors.toMap(loc -> UUID.fromString(loc.getParentIdentifier()),
              LocationChildrenCountProjection::getChildrenCount));
    } else {
      childrenCount = locationRelationshipService.getLocationChildrenCount(
              plan.getLocationHierarchy().getIdentifier())
          .stream().filter(loc -> loc.getParentIdentifier() != null)
          .collect(Collectors.toMap(loc -> UUID.fromString(loc.getParentIdentifier()),
              LocationChildrenCountProjection::getChildrenCount));
    }
    List<PlanLocationDetails> response = locationRelationshipService
        .getAssignedLocationChildrenByLocationParentIdentifierAndPlanIdentifier(
            parentIdentifier, planIdentifier);
    response.forEach(resp -> resp.setChildrenNumber(
        childrenCount.containsKey(resp.getLocation().getIdentifier()) ? childrenCount.get(
            resp.getLocation().getIdentifier()) : 0));
    if (location != null) {
      response = response.stream()
          .peek(planLocationDetail -> planLocationDetail.setParentLocation(location))
          .collect(Collectors.toList());
    }
    return response;
  }

  public PlanLocationDetails getRootLocationByPlanIdentifier(UUID planIdentifier) {
    Plan plan = planService.findPlanByIdentifier(planIdentifier);
    Map<UUID, Long> childrenCount = locationRelationshipService.getLocationChildrenCount(
            plan.getLocationHierarchy().getIdentifier())
        .stream().filter(loc -> loc.getParentIdentifier() != null)
        .collect(Collectors.toMap(loc -> UUID.fromString(loc.getParentIdentifier()),
            LocationChildrenCountProjection::getChildrenCount));
    PlanLocationDetails response = locationRelationshipService.getRootLocationDetailsByPlanId(
        planIdentifier);
    response.setChildrenNumber(
        childrenCount.containsKey(response.getLocation().getIdentifier()) ? childrenCount.get(
            response.getLocation().getIdentifier()) : 0);
    return response;
  }

  public Set<Location> getAssignedLocationsFromPlanAssignments(
      Set<PlanAssignment> planAssignments) {
    return planAssignments.stream().map(PlanAssignment::getPlanLocations)
        .map(PlanLocations::getLocation).collect(Collectors.toSet());
  }

  public LocationCoordinatesProjection getLocationCentroidCoordinatesByIdentifier(
      UUID locationIdentifier) {
    return locationRepository.getLocationCentroidCoordinatesByIdentifier(locationIdentifier);
  }

  public PlanLocationDetails getLocationDetailsByIdentifierAndPlanIdentifier(
      UUID locationIdentifier, UUID planIdentifier) {
    Plan plan = planService.findPlanByIdentifier(planIdentifier);
    PlanLocationDetails details = locationRepository.getLocationDetailsByIdentifierAndPlanIdentifier(
        locationIdentifier, plan.getIdentifier());

    details.setParentLocation(
        locationRelationshipService.findParentLocationByLocationIdAndHierarchyId(locationIdentifier,
            plan.getLocationHierarchy().getIdentifier()));
    return details;
  }

  public Location getLocationParent(Location location, LocationHierarchy locationHierarchy) {
    return locationRelationshipService.getLocationParent(location, locationHierarchy);
  }

  public Location getLocationParentByLocationIdentifierAndHierarchyIdentifier(UUID locationIdentifier, UUID locationHierarchyIdentifier) {
    return locationRelationshipService.getLocationParentByLocationIdentifierAndHierarchyIdentifier(locationIdentifier, locationHierarchyIdentifier);
  }

  public List<UUID> getAllLocationChildren(UUID locationIdentifier, UUID hierarchyIdentifier) {
    return locationRepository.getAllLocationChildren(locationIdentifier, hierarchyIdentifier);
  }

  public List<Location> getLocationsByPeople(UUID personIdentifier) {
    return locationRepository.getLocationsByPeople_Identifier(personIdentifier);
  }
}
