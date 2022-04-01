package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import com.revealprecision.revealserver.persistence.repository.GeographicLevelRepository;
import com.revealprecision.revealserver.persistence.repository.LocationHierarchyRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRelationshipRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.geometry.Point;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class LocationRelationshipService {

  private final LocationRelationshipRepository locationRelationshipRepository;
  private final GeographicLevelRepository geographicLevelRepository;
  private final LocationRepository locationRepository;
  private final LocationHierarchyRepository locationHierarchyRepository;
  private final RestHighLevelClient client;

  @Autowired
  public LocationRelationshipService(LocationRelationshipRepository locationRelationshipRepository,
      GeographicLevelRepository geographicLevelRepository, LocationRepository locationRepository,
      LocationHierarchyRepository locationHierarchyRepository, RestHighLevelClient client) {
    this.locationRelationshipRepository = locationRelationshipRepository;
    this.geographicLevelRepository = geographicLevelRepository;
    this.locationRepository = locationRepository;
    this.locationHierarchyRepository = locationHierarchyRepository;
    this.client = client;
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
      if (nodePosition < locationHierarchy.getNodeOrder().size() && nodePosition >= 0) {
        var parentGeographicLevelName = locationHierarchy.getNodeOrder()
            .get(nodePosition);

        var parentGeographicLevel = geographicLevelRepository.findByName(parentGeographicLevelName);

        var upperGeographicLevelLocations = locationRepository
            .findByGeographicLevelIdentifier(parentGeographicLevel.get()
                .getIdentifier());

        upperGeographicLevelLocations.stream().forEach(
            parentLocation -> createParentChildRelationship(parentLocation, location,
                locationHierarchy));
      } else if (nodePosition == -1) {
        createRelationshipForRoot(location, locationHierarchy);
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

  public Optional<List<LocationRelationship>> getLocationRelationshipsWithoutStructure(LocationHierarchy locationHierarchy){
    return locationRelationshipRepository.findByLocationHierarchyWithoutStructures(locationHierarchy.getIdentifier());
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

  public List<UUID> findLocationRelationshipUiidsByParentLocationIdentifier(
      UUID parentLocationIdentifier) {
    return locationRelationshipRepository.findLocationRelationshipUuidsByParentLocation_Identifier(
        parentLocationIdentifier);
  }

  @Async("getAsyncExecutorTest")
  public void createRelationshipForImportedLocation(Location location) throws IOException {
    List<LocationHierarchy> locationHierarchies = locationHierarchyRepository
        .findLocationHierarchiesByNodeOrderContaining(location.getGeographicLevel().getName());
    for (var locationHierarchy : locationHierarchies) {

      Integer nodePosition =
          locationHierarchy.getNodeOrder().indexOf(location.getGeographicLevel().getName()) - 1;
      if (nodePosition < locationHierarchy.getNodeOrder().size() && nodePosition >= 0) {
        String parentGeographicLevelName = locationHierarchy.getNodeOrder()
            .get(nodePosition);

        String centroid = locationRepository.getCentroid(location.getIdentifier());
        centroid = centroid.substring(6).replace(")","");
        double x = Double.parseDouble(centroid.split(" ")[0]);
        double y = Double.parseDouble(centroid.split(" ")[1]);
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.matchQuery("level", parentGeographicLevelName));
        boolQuery.filter(QueryBuilders.geoShapeQuery("geometry", new Point(x,  y)).relation(
            ShapeRelation.CONTAINS));
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(boolQuery);
        SearchRequest searchRequest = new SearchRequest("location");
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        UUID parentLocation = UUID.fromString(searchResponse.getHits().getAt(0).getId());
        if(parentLocation != null) {
          Location parentLoc = Location.builder().identifier(parentLocation).build();
          LocationRelationship locationRelationshipToSave = LocationRelationship.builder()
              .parentLocation(parentLoc)
              .location(location)
              .locationHierarchy(locationHierarchy)
              .build();
          locationRelationshipToSave.setEntityStatus(EntityStatus.ACTIVE);
          locationRelationshipRepository.save(locationRelationshipToSave);
        }
      } else if (nodePosition == -1) {
        createRelationshipForRoot(location, locationHierarchy);
      }
    }
  }

  public List<Location> getChildrenLocations(UUID hierarchyIdentifier, UUID locationIdentifier) {
    List<Location> children = locationRelationshipRepository.getChildren(hierarchyIdentifier,locationIdentifier);
    return children;
  }
}
