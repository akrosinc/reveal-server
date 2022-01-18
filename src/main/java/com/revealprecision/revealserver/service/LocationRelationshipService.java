package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.repository.GeographicLevelRepository;
import com.revealprecision.revealserver.persistence.repository.LocationHierarchyRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRelationshipRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LocationRelationshipService {

  private LocationRelationshipRepository locationRelationshipRepository;
  private GeographicLevelRepository geographicLevelRepository;
  private LocationRepository locationRepository;
  private LocationHierarchyRepository locationHierarchyRepository;

  @Autowired
  public LocationRelationshipService(LocationRelationshipRepository locationRelationshipRepository,
      GeographicLevelRepository geographicLevelRepository, LocationRepository locationRepository,
      LocationHierarchyRepository locationHierarchyRepository) {
    this.locationRelationshipRepository = locationRelationshipRepository;
    this.geographicLevelRepository = geographicLevelRepository;
    this.locationRepository = locationRepository;
    this.locationHierarchyRepository = locationHierarchyRepository;
  }

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
            createParentChildRelationship(location, potentialChild, locationHierarchy);
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

  public void updateLocationRelationshipsForNewLocation(Location location) {
    var locationHierarchies = locationHierarchyRepository
        .findLocationHierarchiesByNodeOrderContaining(location.getGeographicLevel().getName());
    for (var locationHierarchy : locationHierarchies) {

      Integer nodePosition =
          locationHierarchy.getNodeOrder().indexOf(location.getGeographicLevel().getName()) - 1;
      if (nodePosition < locationHierarchy.getNodeOrder().size() && nodePosition > 0) {
        var parentGeographicLevelName = locationHierarchy.getNodeOrder()
            .get(nodePosition);

        var parentGeographicLevel = geographicLevelRepository.findByName(parentGeographicLevelName);

        var upperGeographicLevelLocations = locationRepository
            .findByGeographicLevelIdentifier(parentGeographicLevel.get()
                .getIdentifier());

        upperGeographicLevelLocations.stream().forEach(
            parentLocation -> createParentChildRelationship(parentLocation, location,
                locationHierarchy));
      }

      nodePosition =
          locationHierarchy.getNodeOrder().indexOf(location.getGeographicLevel().getName()) + 1;
      if (nodePosition < locationHierarchy.getNodeOrder().size()) {
        var lowerLevelGeographicLevelName = locationHierarchy.getNodeOrder()
            .get(nodePosition);
        var lowerGeographicLevel = geographicLevelRepository
            .findByName(lowerLevelGeographicLevelName);
        var lowerGeographicLevelLocations = locationRepository
            .findByGeographicLevelIdentifier(lowerGeographicLevel.get()
                .getIdentifier());

        lowerGeographicLevelLocations.stream().forEach(
            childLocation -> createParentChildRelationship(location, childLocation,
                locationHierarchy));

      }
    }
  }

  private void createParentChildRelationship(Location parentLocation, Location childLocation,
      LocationHierarchy locationHierarchy) {
    ObjectMapper mapper = new ObjectMapper();
    String parentGeometry = null;
    String childGeometry = null;
    try {
      parentGeometry = mapper.writeValueAsString(parentLocation.getGeometry());
      childGeometry = mapper.writeValueAsString(childLocation.getGeometry());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    if (locationRelationshipRepository.hasParentChildRelationship(parentGeometry, childGeometry)) {
      var ancestry = getAncestryFromParentLocation(parentLocation, locationHierarchy);
      var locationRelationshipToSave = LocationRelationship.builder().parentLocation(parentLocation)
          .location(childLocation)
          .locationHierarchy(locationHierarchy).ancestry(ancestry)
          .build();
      locationRelationshipToSave.setEntityStatus(EntityStatus.ACTIVE);
      locationRelationshipRepository.save(locationRelationshipToSave);
    }
  }


  private List<UUID> getAncestryFromParentLocation(Location parentLocation,
      LocationHierarchy locationHierarchy) {
    List<UUID> ancestry = new ArrayList<>();
    if (parentLocation == null) {
      return ancestry;
    }
    Optional<LocationRelationship> locationRelationshipOptional = locationRelationshipRepository
        .findByLocationHierarchyIdentifierAndLocationIdentifier(
            locationHierarchy.getIdentifier(), parentLocation.getIdentifier());
    if (locationRelationshipOptional.isPresent()) {
      ancestry.addAll(locationRelationshipOptional.get().getAncestry());
    }
    ancestry.add(parentLocation.getIdentifier());
    return ancestry;
  }

  public Optional<List<LocationRelationship>> getLocationRelationshipsForLocationHierarchy(
      LocationHierarchy locationHierarchy) {
    return locationRelationshipRepository
        .findByLocationHierarchyIdentifier(locationHierarchy.getIdentifier());
  }
}
