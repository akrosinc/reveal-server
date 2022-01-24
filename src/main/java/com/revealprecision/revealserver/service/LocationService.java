package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel.Fields;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
public class LocationService {

  private LocationRepository locationRepository;
  private GeographicLevelService geographicLevelService;
  private LocationRelationshipService locationRelationshipService;
  private JobScheduler jobScheduler;

  public LocationService(LocationRepository locationRepository,
      GeographicLevelService geographicLevelService,
      LocationRelationshipService locationRelationshipService,
      JobScheduler jobScheduler) {
    this.locationRepository = locationRepository;
    this.geographicLevelService = geographicLevelService;
    this.locationRelationshipService = locationRelationshipService;
    this.jobScheduler = jobScheduler;
  }

  public Location createLocation(LocationRequest locationRequest) {
    Optional<GeographicLevel> geographicLevel = geographicLevelService
        .findByName(locationRequest.getProperties().getGeographicLevel());
    if (!geographicLevel.isPresent()) {
      throw new NotFoundException(
          Pair.of(Fields.name, locationRequest.getProperties().getGeographicLevel()),
          GeographicLevel.class);
    }
    var locationToSave = Location.builder().geographicLevel(geographicLevel.get())
        .type(locationRequest.getType())
        .geometry(locationRequest.getGeometry()).name(locationRequest.getProperties().getName())
        .status(locationRequest.getProperties().getStatus())
        .externalId(locationRequest.getProperties().getExternalId()).build();
    locationToSave.setEntityStatus(EntityStatus.ACTIVE);
    var savedLocation = locationRepository.save(locationToSave);
    jobScheduler.enqueue(
        () -> locationRelationshipService.updateLocationRelationshipsForNewLocation(savedLocation));
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
    GeographicLevel geographicLevel = geographicLevelService
        .findByName(locationRequest.getProperties().getGeographicLevel())
        .orElseThrow(() -> new NotFoundException(
            Pair.of(Fields.name, locationRequest.getProperties().getGeographicLevel()),
            GeographicLevel.class));
    return locationRepository.save(location.update(locationRequest, geographicLevel));
  }

  public List<Location> getAllByIdentifiers(List<UUID> identifiers) {
    return locationRepository.getAllByIdentifiers(identifiers);
  }
}
