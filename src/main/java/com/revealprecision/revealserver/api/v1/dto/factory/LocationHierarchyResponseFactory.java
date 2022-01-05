package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.GeoTree;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationHierarchyResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationPropertyResponse;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.util.TreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class LocationHierarchyResponseFactory {

  public static LocationHierarchyResponse fromEntityWithoutTree(
      LocationHierarchy locationHierarchy) {
    return LocationHierarchyResponse.builder().identifier(locationHierarchy.getIdentifier())
        .nodeOrder(locationHierarchy.getNodeOrder()).build();
  }

  public static LocationHierarchyResponse fromEntityWithTree(LocationHierarchy locationHierarchy) {

    List<GeoTreeResponse> geoTree = generateLocationTreeResponse(
        locationHierarchy.getLocationRelationships()); //TODO: cache this type of response

    return LocationHierarchyResponse.builder().identifier(locationHierarchy.getIdentifier())
        .geoTree(geoTree)
        .nodeOrder(locationHierarchy.getNodeOrder()).build();
  }

  private static List<GeoTreeResponse> generateLocationTreeResponse(
      List<LocationRelationship> locationRelationships) {
    var rootLocations = locationRelationships.stream()
        .filter(locationRelationship -> locationRelationship.getParentLocation() == null).collect(
            Collectors.toList());
    GeoTree geoTree = new GeoTree();
    geoTree.buildTreeFromList(locationRelationships);
    //TODO: split geotrees by country or root node,(fillter with ancestry where there is root node,and include root note as well)
    List<GeoTreeResponse> locationHierarchies = new ArrayList<>();

    List<GeoTreeResponse> geoTreeResponse = generateGeoTreeResponseFromTree(
        geoTree.getLocationsHierarchy());
    if (!geoTreeResponse.isEmpty()) {
      locationHierarchies.add(geoTreeResponse.get(0));
    }
    return locationHierarchies;
  }

  private static List<GeoTreeResponse> generateGeoTreeResponseFromTree(
      Map<UUID, TreeNode<UUID, Location>> map) {
    List<GeoTreeResponse> geoTreeResponses = new ArrayList<>();

    for (Map.Entry<UUID, TreeNode<UUID, Location>> entry : map.entrySet()) {
      List<GeoTreeResponse> foundLocations = getTheData(entry.getValue());
      if (!foundLocations.isEmpty()) {
        geoTreeResponses.addAll(foundLocations);
      }

    }

    return geoTreeResponses;
  }

  private static List<GeoTreeResponse> getTheData(TreeNode<UUID, Location> node) {
    List<GeoTreeResponse> allLocationData = new ArrayList<>();

    var locationPropertyResponse = LocationPropertyResponse.builder().name(node.getNode().getName())
        .externalId(node.getNode().getExternalId()).status(node.getNode().getStatus())
        .geographicLevel(node.getNode().getGeographicLevel().getName()).build();
    var geoTreeResponse = GeoTreeResponse.builder().identifier(node.getId())
        .geometry(node.getNode().getGeometry()).properties(locationPropertyResponse).build();
    List<GeoTreeResponse> children = new ArrayList<>();
    if (node.getChildren() != null) {
      for (Map.Entry<UUID, TreeNode<UUID, Location>> childEntry : node.getChildren().entrySet()) {
        List<GeoTreeResponse> childLocations = getTheData(childEntry.getValue());
        if (!childLocations.isEmpty()) {
          children.addAll(childLocations);
        }
      }
    }

    geoTreeResponse.setChildren(children);
    allLocationData.add(geoTreeResponse);
    return allLocationData;
  }
}
