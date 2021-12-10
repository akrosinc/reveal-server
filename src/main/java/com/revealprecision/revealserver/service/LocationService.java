package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel.Fields;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class LocationService {

  private LocationRepository locationRepository;
  private GeographicLevelService geographicLevelService;

  public LocationService(LocationRepository locationRepository,
      GeographicLevelService geographicLevelService) {
    this.locationRepository = locationRepository;
    this.geographicLevelService = geographicLevelService;
  }

  public Location createLocation(LocationRequest locationRequest) {
    Optional<GeographicLevel> geographicLevel = geographicLevelService
        .findByName(locationRequest.getProperties().getGeographicLevel());
    if (!geographicLevel.isPresent()) {
      throw new NotFoundException(
          Pair.of(Fields.name, locationRequest.getProperties().getGeographicLevel()),
          GeographicLevel.class);
    }
    Location saveLocation = new Location();
    saveLocation.setGeographicLevel(geographicLevel.get());
    saveLocation.setType(locationRequest.getType());
    saveLocation.setGeometry(locationRequest.getGeometry());
    saveLocation.setName(locationRequest.getProperties().getName());
    saveLocation.setStatus(locationRequest.getProperties().getStatus());
    saveLocation.setExternalId(locationRequest.getProperties().getExternalId());
    return locationRepository.save(saveLocation);
  }

  public Location findByIdentifier(UUID identifier) {
    return locationRepository.findById(identifier).orElseThrow(
        () -> new NotFoundException(Pair.of(Location.Fields.identifier, identifier),
            Location.class));
  }

  public Page<Location> getLocations(Integer pageNumber, Integer pageSize) {
    return locationRepository.findAll(PageRequest.of(pageNumber, pageSize));
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


}
