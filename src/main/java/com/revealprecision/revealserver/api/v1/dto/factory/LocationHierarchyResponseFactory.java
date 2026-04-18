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
    return LocationHierarchyResponse.builder().identifier(locationHierarchy.getIdentifier().toString())
        .name(locationHierarchy.getName())
        .hierarchyStatus(locationHierarchy.getHierarchyStatus())
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
    return LocationHierarchyResponse.builder().identifier(locationHierarchy.getIdentifier().toString())
        .name(locationHierarchy.getName())
        .geoTree(geoTree)
        .nodeOrder(locationHierarchy.getNodeOrder()).build();
  }

  public static List<GeoTreeResponse> generateLocationTreeResponseWithoutGeom(List<LocationRelationship> locationRelationships){
    return generateLocationTreeResponse(locationRelationships, false);
  }

  private static List<GeoTreeResponse> generateLocationTreeResponse(
      List<LocationRelationship> locationRelationships, Boolean includeGeometry) {

    UUID rootParentId = UUID.fromString("00000000-0000-0000-0000-000000000000");

    List<GeoTreeResponse> geoTreeResponses = locationRelationships.stream()
        .map(lr -> GeoTreeResponse.builder()
            .identifier(lr.getLocation().getIdentifier())
            .geometry(includeGeometry ? lr.getLocation().getGeometry() : null)
            .properties(LocationPropertyResponse.builder()
                .parentIdentifier((lr.getParentLocation() == null) ? rootParentId
                    : lr.getParentLocation().getIdentifier())
                .name(lr.getLocation().getName())
                .externalId(lr.getLocation().getExternalId())
                .status(lr.getLocation().getStatus())
                .geographicLevel(lr.getLocation().getGeographicLevel().getName())
                .build())
            .build()).collect(Collectors.toList());

    Map<UUID, List<GeoTreeResponse>> geoTreeHierarchy = geoTreeResponses.stream()
        .collect(Collectors.groupingBy(lr -> lr.getProperties().getParentIdentifier(),
            Collectors.mapping(lr -> lr, Collectors.toList())));

    geoTreeResponses.forEach(gt -> gt.setChildren(
        geoTreeHierarchy.get(gt.getIdentifier()) == null ? new ArrayList<>()
            : geoTreeHierarchy.get(gt.getIdentifier())));

    return geoTreeHierarchy.getOrDefault(rootParentId, new ArrayList<>());
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
