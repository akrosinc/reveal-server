package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.IdentifierNameResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.IdentifierNameResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationHierarchyResponse;
import com.revealprecision.revealserver.config.InstanceContext;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.repository.InstanceLocationRepository;
import com.revealprecision.revealserver.persistence.repository.InstanceRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstanceLocationService {

  private final InstanceRepository instanceRepository;
  private final InstanceLocationRepository instanceLocationRepository;
  private final LocationHierarchyService locationHierarchyService;
  private final LocationRelationshipService locationRelationshipService;

  private Instance findInstanceById(UUID identifier) {
    return instanceRepository.findById(identifier)
        .orElseThrow(
            () -> new NotFoundException(Pair.of("identifier", identifier), Instance.class));
  }

  public LocationHierarchy getInstanceHierarchy(UUID instanceIdentifier) {
    Instance instance = findInstanceById(instanceIdentifier);
    return instance.getLocationHierarchy();
  }

  @Cacheable(value = "instance-hierarchy-tree", key = "#instanceIdentifier", condition = "#instanceIdentifier != null")
  public LocationHierarchyResponse getInstanceHierarchyTreeResponse(UUID instanceIdentifier) {
    if (instanceIdentifier == null) {
      // Get base hierarchy and build full geo tree
      LocationHierarchy baseHierarchy = locationHierarchyService.getBaseLocationHierarchy();
      List<GeoTreeResponse> hierarchyTree = locationHierarchyService.getGeoTreeFromLocationHierarchy(baseHierarchy, true);

      return LocationHierarchyResponse.builder().identifier(baseHierarchy.getIdentifier().toString())
          .name(baseHierarchy.getName())
          .geoTree(hierarchyTree)
          .nodeOrder(baseHierarchy.getNodeOrder()).build();
    }
    Instance instance = findInstanceById(instanceIdentifier);

    LocationHierarchy instanceLocationHierarchy = instance.getLocationHierarchy();

    List<GeoTreeResponse> hierarchyTree = getAssignedInstanceAreasTree(instance);
    return LocationHierarchyResponse.builder().identifier(instanceLocationHierarchy.getIdentifier().toString())
        .name(instanceLocationHierarchy.getName())
        .geoTree(hierarchyTree)
        .nodeOrder(instanceLocationHierarchy.getNodeOrder()).build();
  }

  public LocationHierarchyResponse getInstanceHierarchyWithGroups(UUID instanceIdentifier) {
    if (instanceIdentifier == null) {
      // Get base hierarchy and build full geo tree
      LocationHierarchy baseHierarchy = locationHierarchyService.getBaseLocationHierarchy();
      List<GeoTreeResponse> hierarchyTree = locationHierarchyService.getGeoTreeFromLocationHierarchy(baseHierarchy, true);

      return LocationHierarchyResponse.builder().identifier(baseHierarchy.getIdentifier().toString())
          .name(baseHierarchy.getName())
          .geoTree(hierarchyTree)
          .nodeOrder(baseHierarchy.getNodeOrder()).build();
    }
    Instance instance = findInstanceById(instanceIdentifier);

    LocationHierarchy instanceLocationHierarchy = instance.getLocationHierarchy();

    List<GeoTreeResponse> hierarchyTree = getAssignedInstanceAreasTree(instanceIdentifier);
    return LocationHierarchyResponse.builder().identifier(instanceLocationHierarchy.getIdentifier().toString())
        .name(instanceLocationHierarchy.getName())
        .geoTree(hierarchyTree)
        .nodeOrder(instanceLocationHierarchy.getNodeOrder()).build();
  }

  public List<IdentifierNameResponse> getAssignedInstanceAreas() {
    UUID instanceIdentifier = InstanceContext.get();
    return getAssignedInstanceAreas(instanceIdentifier);
  }

  public List<IdentifierNameResponse> getAssignedInstanceAreas(UUID instanceIdentifier) {
    return instanceLocationRepository.getAreasIdNamesByInstance(instanceIdentifier).stream()
        .map(IdentifierNameResponseFactory::toIdentifierNameResponse).collect(Collectors.toList());
  }

  public List<GeoTreeResponse> getAssignedInstanceAreasTree(List<String> nodeList) {
    return getAssignedInstanceAreasTree((UUID) null, nodeList);
  }

  public List<GeoTreeResponse> getAssignedInstanceAreasTree() {
    return getAssignedInstanceAreasTree((UUID) null, null);
  }

  public List<GeoTreeResponse> getAssignedInstanceAreasTree(UUID instanceIdentifier) {
    return getAssignedInstanceAreasTree(instanceIdentifier, null);
  }

  public List<GeoTreeResponse> getAssignedInstanceAreasTree(Instance instance) {
    return getAssignedInstanceAreasTree(instance, null);
  }

  public List<GeoTreeResponse> getAssignedInstanceAreasTree(UUID instanceIdentifier, List<String> nodeList) {
    Instance instance;

    if (instanceIdentifier == null) {
      instanceIdentifier = InstanceContext.get();
    }

    instance = findInstanceById(instanceIdentifier);

    return getAssignedInstanceAreasTree(instance, nodeList);
  }

  public List<GeoTreeResponse> getAssignedInstanceAreasTree(Instance instance, List<String> nodeList) {
    List<IdentifierNameResponse> instancesAreas = getAssignedInstanceAreas(instance.getIdentifier());

    List<UUID> instancesAreasIds = instancesAreas.stream()
        .map(IdentifierNameResponse::getIdentifier)
        .collect(Collectors.toList());

    return locationRelationshipService.getFilteredGeoTreeByLocationIds(instance.getLocationHierarchy(),
        instancesAreasIds, nodeList);
  }
}
