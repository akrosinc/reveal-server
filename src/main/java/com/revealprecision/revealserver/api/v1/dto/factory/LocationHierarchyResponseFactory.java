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
    return LocationHierarchyResponse.builder().identifier(locationHierarchy.getIdentifier())
        .name(locationHierarchy.getName())
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

  public static LocationHierarchyResponse fromEntityWithTree(LocationHierarchy locationHierarchy,Boolean includeGeometry) {

    List<GeoTreeResponse> geoTree = generateLocationTreeResponse(
        locationHierarchy.getLocationRelationships(),includeGeometry); //TODO: cache this type of response
    return LocationHierarchyResponse.builder().identifier(locationHierarchy.getIdentifier())
        .name(locationHierarchy.getName())
        .geoTree(geoTree)
        .nodeOrder(locationHierarchy.getNodeOrder()).build();
  }

  private static List<GeoTreeResponse> generateLocationTreeResponse(
      List<LocationRelationship> locationRelationships,Boolean includeGeometry) {
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
          geoTree.getLocationsHierarchy(),includeGeometry);
      if (!geoTreeResponse.isEmpty()) {
        locationHierarchies.add(geoTreeResponse.get(0));
      }
    }
    return locationHierarchies;
  }



  public  static Page<GeoTreeResponse> generatePageableGeoTreeResponse(List<GeoTreeResponse> geoTreeResponses,Pageable pageable,String search){

      if (geoTreeResponses != null) {
        final int start = (int)pageable.getOffset();
        final int end = Math.min((start + pageable.getPageSize()), geoTreeResponses.size());
        return new PageImpl<>(geoTreeResponses.subList(start,end).stream().filter(geoTreeResponse -> geoTreeResponse.getProperties().getName().contains(search)).collect(
                Collectors.toList()),pageable,geoTreeResponses.size());
      }
      return new PageImpl<>(new ArrayList<>(), pageable, 0);
  }

  public static List<GeoTreeResponse> generateGeoTreeResponseFromTree(
      Map<UUID, TreeNode<UUID, Location>> map,Boolean includeGeometry) {
    List<GeoTreeResponse> geoTreeResponses = new ArrayList<>();

    for (Map.Entry<UUID, TreeNode<UUID, Location>> entry : map.entrySet()) {
      List<GeoTreeResponse> foundLocations = buildGeoResponseFromNode(entry.getValue(),includeGeometry);
      if (!foundLocations.isEmpty()) {
        geoTreeResponses.addAll(foundLocations);
      }

    }

    return geoTreeResponses;
  }

  public static List<GeoTreeResponse> buildGeoResponseFromNode(TreeNode<UUID, Location> node,Boolean includeGeometry) {
    List<GeoTreeResponse> allLocationData = new ArrayList<>();

    var locationPropertyResponse = LocationPropertyResponse.builder()
        .name(node.getNode().getName())
        .externalId(node.getNode()
            .getExternalId())
        .status(node.getNode().getStatus())
        .geographicLevel(node.getNode()
            .getGeographicLevel()
            .getName())
        .parentIdentifier(node.getParent())
        .build();
    var geoTreeResponse = GeoTreeResponse.builder().identifier(node.getId())
       .geometry(includeGeometry ? node.getNode().getGeometry() : null)
        .properties(locationPropertyResponse).build();
    List<GeoTreeResponse> children = new ArrayList<>();
    if (node.getChildren() != null) {
      for (Map.Entry<UUID, TreeNode<UUID, Location>> childEntry : node.getChildren().entrySet()) {
        List<GeoTreeResponse> childLocations = buildGeoResponseFromNode(childEntry.getValue(),includeGeometry);
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
