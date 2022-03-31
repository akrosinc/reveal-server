package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import com.revealprecision.revealserver.persistence.projection.LocationCoordinatesProjection;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import java.util.Collection;
import java.util.List;
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

  public Location createLocation(LocationRequest locationRequest) {
    GeographicLevel geographicLevel = geographicLevelService.findByName(
        locationRequest.getProperties().getGeographicLevel());
    var locationToSave = Location.builder().geographicLevel(geographicLevel)
        .type(locationRequest.getType())
        .geometry(locationRequest.getGeometry()).name(locationRequest.getProperties().getName())
        .status(locationRequest.getProperties().getStatus())
        .externalId(locationRequest.getProperties().getExternalId()).build();
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
      LocationHierarchy locationHierarchy) {
    List<LocationRelationship> locationRelationships = locationHierarchy.getLocationRelationships();
    List<Location> locations = locationRelationships.stream().filter(
        locationRelationship -> locationRelationship.getParentLocation() != null
            && parentIdentifiers
            .contains(locationRelationship.getParentLocation().getIdentifier()))
        .map(LocationRelationship::getLocation).collect(
            Collectors.toList());

    return locations.stream()
        .filter(location -> location.getGeographicLevel().getName().equalsIgnoreCase("structure"))
        .collect(
            Collectors.toList());//TODO: to update once we figure the target level part.
  }

  public List<PlanLocationDetails> getLocationsByParentIdentifierAndPlanIdentifier(
      UUID parentIdentifier,
      UUID planIdentifier) {

    return locationRelationshipService
        .getLocationChildrenByLocationParentIdentifierAndPlanIdentifier(
            parentIdentifier, planIdentifier);
  }


  public Set<Location> getAssignedLocationsFromPlans(Set<Plan> assignedPlans) {
    Set<Location> assignedLocations = assignedPlans.stream().map(Plan::getPlanLocations)
        .flatMap(Collection::stream).map(PlanLocations::getLocation).collect(Collectors.toSet());
    return assignedLocations;
  }

  public LocationCoordinatesProjection getLocationCentroidCoordinatesByIdentifier(
      UUID locationIdentifier) {
    return locationRepository.getLocationCentroidCoordinatesByIdentifier(locationIdentifier);
  }
}
