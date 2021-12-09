package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.repository.GeographicLevelRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRelationshipRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import java.util.ArrayList;
import java.util.Arrays;
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

  @Autowired
  public LocationRelationshipService(LocationRelationshipRepository locationRelationshipRepository,
      GeographicLevelRepository geographicLevelRepository, LocationRepository locationRepository) {
    this.locationRelationshipRepository = locationRelationshipRepository;
    this.geographicLevelRepository = geographicLevelRepository;
    this.locationRepository = locationRepository;
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

    geographicLevelToLocationsMap.entrySet().stream().forEach(item -> {
      List<Location> parentLocations = item.getValue();
      if (geographicLevels.size() > geographicLevels.indexOf(item.getKey()) + 1) {
        List<Location> potentialChildren = geographicLevelToLocationsMap
            .get(geographicLevels.get(geographicLevels.indexOf(item.getKey()) + 1));
        ObjectMapper mapper = new ObjectMapper();
        for (Location location : parentLocations) {
          for (Location potentialChild : potentialChildren) {
            String parentGeometry = null;
            String childGeometry = null;

            try {
              parentGeometry = mapper.writeValueAsString(location.getGeometry());
              childGeometry = mapper.writeValueAsString(potentialChild.getGeometry());

            } catch (JsonProcessingException e) {
              e.printStackTrace();
            }
            if (locationRepository.hasParentChildRelationship(parentGeometry, childGeometry)) {
              List<UUID> ancestry = new ArrayList<>();
              Optional<LocationRelationship> locationRelationshipOptional = locationRelationshipRepository.findByLocationHierarchyIdentifierAndLocationIdentifier(locationHierarchy.getIdentifier(),location.getIdentifier());
              if(locationRelationshipOptional.isPresent()){
                ancestry.addAll(locationRelationshipOptional.get().getAncestry());
              }
              ancestry.add(location.getIdentifier());

              LocationRelationship locationRelationship = LocationRelationship.builder()
                  .parentIdentifier(location.getIdentifier())
                  .locationHierarchyIdentifier(locationHierarchy.getIdentifier())
                  .locationIdentifier(potentialChild.getIdentifier())
                  .ancestry(ancestry).build();
              locationRelationshipRepository.save(locationRelationship);
            }

          }
        }
      }
    });

  }

}
