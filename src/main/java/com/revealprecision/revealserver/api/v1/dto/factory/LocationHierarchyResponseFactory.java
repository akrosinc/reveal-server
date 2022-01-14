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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class LocationHierarchyResponseFactory {

  public static LocationHierarchyResponse fromEntityWithoutTree(
      LocationHierarchy locationHierarchy) {
    return LocationHierarchyResponse.builder().identifier(locationHierarchy.getIdentifier()).name(locationHierarchy.getName())
        .nodeOrder(locationHierarchy.getNodeOrder()).build();
  }


  public static Page<LocationHierarchyResponse> fromEntityPage(
      Page<LocationHierarchy> locationHierarchies,
      Pageable pageable) {

    var locationHierarchyResponseContent = locationHierarchies.getContent().stream()
        .map(LocationHierarchyResponseFactory::fromEntityWithoutTree)
        .collect(Collectors.toList());
    return new PageImpl<>(locationHierarchyResponseContent, pageable,
        locationHierarchies.getTotalElements());
  }

  public static LocationHierarchyResponse fromEntityWithTree(LocationHierarchy locationHierarchy) {

    List<GeoTreeResponse> geoTree = generateLocationTreeResponse(
        locationHierarchy.getLocationRelationships()); //TODO: cache this type of response

    return LocationHierarchyResponse.builder().identifier(locationHierarchy.getIdentifier()).name(locationHierarchy.getName())
        .geoTree(geoTree)
        .nodeOrder(locationHierarchy.getNodeOrder()).build();
  }

  private static List<GeoTreeResponse> generateLocationTreeResponse(
      List<LocationRelationship> locationRelationships) {
    var rootLocations = locationRelationships.stream()
        .filter(locationRelationship -> locationRelationship.getParentLocation() == null)
        .map(locationRelationship -> locationRelationship.getLocation()).collect(
            Collectors.toList());

    List<GeoTreeResponse> locationHierarchies = new ArrayList<>();
    for (var location : rootLocations) {
      GeoTree geoTree = new GeoTree();
      var locationRelationshipsSplitByRootLocation = locationRelationships.stream().filter(
          locationRelationship -> locationRelationship.getLocation().equals(location)
              || locationRelationship.getAncestry().contains(location.getIdentifier())).collect(
          Collectors.toList());
      geoTree.buildTreeFromList(locationRelationshipsSplitByRootLocation);
      List<GeoTreeResponse> geoTreeResponse = generateGeoTreeResponseFromTree(
          geoTree.getLocationsHierarchy());
      if (!geoTreeResponse.isEmpty()) {
        locationHierarchies.add(geoTreeResponse.get(0));
      }
    }
    return locationHierarchies;
  }

  private static List<GeoTreeResponse> generateGeoTreeResponseFromTree(
      Map<UUID, TreeNode<UUID, Location>> map) {
    List<GeoTreeResponse> geoTreeResponses = new ArrayList<>();

    for (Map.Entry<UUID, TreeNode<UUID, Location>> entry : map.entrySet()) {
      List<GeoTreeResponse> foundLocations = buildGeoResponseFromNode(entry.getValue());
      if (!foundLocations.isEmpty()) {
        geoTreeResponses.addAll(foundLocations);
      }

    }

    return geoTreeResponses;
  }

  private static List<GeoTreeResponse> buildGeoResponseFromNode(TreeNode<UUID, Location> node) {
    List<GeoTreeResponse> allLocationData = new ArrayList<>();

    var locationPropertyResponse = LocationPropertyResponse.builder().name(node.getNode().getName())
        .externalId(node.getNode().getExternalId()).status(node.getNode().getStatus())
        .geographicLevel(node.getNode().getGeographicLevel().getName()).build();
    var geoTreeResponse = GeoTreeResponse.builder().identifier(node.getId())
        .geometry(node.getNode().getGeometry()).properties(locationPropertyResponse).build();
    List<GeoTreeResponse> children = new ArrayList<>();
    if (node.getChildren() != null) {
      for (Map.Entry<UUID, TreeNode<UUID, Location>> childEntry : node.getChildren().entrySet()) {
        List<GeoTreeResponse> childLocations = buildGeoResponseFromNode(childEntry.getValue());
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
