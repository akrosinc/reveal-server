package com.revealprecision.revealserver.service;

import static com.revealprecision.revealserver.constants.LocationConstants.STRUCTURE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.projection.LocationAndHigherParentProjection;
import com.revealprecision.revealserver.persistence.projection.LocationChildrenCountProjection;
import com.revealprecision.revealserver.persistence.projection.LocationMainData;
import com.revealprecision.revealserver.persistence.projection.LocationRelationshipAncestryProjection;
import com.revealprecision.revealserver.persistence.projection.LocationRelationshipProjection;
import com.revealprecision.revealserver.persistence.projection.LocationWithParentProjection;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import com.revealprecision.revealserver.persistence.repository.GeographicLevelRepository;
import com.revealprecision.revealserver.persistence.repository.LiteStructureCountRepository;
import com.revealprecision.revealserver.persistence.repository.LocationCountsRepository;
import com.revealprecision.revealserver.persistence.repository.LocationHierarchyRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRelationshipRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LocationRelationshipService {

  private final LocationRelationshipRepository locationRelationshipRepository;
  private final GeographicLevelRepository geographicLevelRepository;
  private final LocationRepository locationRepository;
  private final LocationHierarchyRepository locationHierarchyRepository;
  private final Environment env;
  private final LocationCountsRepository locationCountsRepository;
  private final LiteStructureCountRepository liteStructureCountRepository;
  private final LocationUtilService locationUtilService;

  @Autowired
  public LocationRelationshipService(LocationRelationshipRepository locationRelationshipRepository,
      GeographicLevelRepository geographicLevelRepository, LocationRepository locationRepository,
      LocationHierarchyRepository locationHierarchyRepository,
      Environment env, LocationCountsRepository locationCountsRepository,
      LiteStructureCountRepository liteStructureCountRepository,
      LocationUtilService locationUtilService) {
    this.locationRelationshipRepository = locationRelationshipRepository;
    this.geographicLevelRepository = geographicLevelRepository;
    this.locationRepository = locationRepository;
    this.locationHierarchyRepository = locationHierarchyRepository;
    this.env = env;
    this.locationCountsRepository = locationCountsRepository;
    this.liteStructureCountRepository = liteStructureCountRepository;
    this.locationUtilService = locationUtilService;
  }


  public Map<String, List<String>> getAncestryMap(List<String> locationIds){

    List<LocationRelationshipAncestryProjection> locationAncestryListsFromLocationIds = locationRelationshipRepository
        .getLocationAncestryListsFromLocationIds(
            locationIds.stream().map(UUID::fromString).collect(Collectors.toList()));

    Map<String, List<String>> collect = locationAncestryListsFromLocationIds
        .stream().collect(
            Collectors.groupingBy(LocationRelationshipAncestryProjection::getLocationIdentifier,
                Collectors.mapping(LocationRelationshipAncestryProjection::getAncestor,
                    Collectors.toList())));

    return collect;
    
  }
  

  public List<LocationWithParentProjection> getChildrenByGeoLevelNameWithinLocationListHierarchyAndServerVersion(
      List<UUID> parentIdentifiers,
      UUID locationHierarchyIdentifier, Long serverVersion, String geographicLevelName) {
    return locationRelationshipRepository.getChildrenByGeoLevelNameWithinLocationListHierarchyAndServerVersion(
        geographicLevelName, parentIdentifiers,
        locationHierarchyIdentifier, serverVersion);
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



  public void updateLocationRelationshipsForNewLocation(Location location) throws IOException {
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

        for (Location parentLocation : upperGeographicLevelLocations) {
          createParentChildRelationship(parentLocation, location,
              locationHierarchy);
        }
      } else if (nodePosition == -1) {
        locationUtilService.createRelationshipForRoot(location, locationHierarchy);
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

        for (Location childLocation : lowerGeographicLevelLocations) {
          createParentChildRelationship(location, childLocation,
              locationHierarchy);
        }
      }
    }
  }

  private void createParentChildRelationship(Location parentLocation, Location childLocation,
      LocationHierarchy locationHierarchy) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String parentGeometry = null;
    String childGeometry = null;
    try {
      parentGeometry = mapper.writeValueAsString(parentLocation.getGeometry());
      childGeometry = mapper.writeValueAsString(childLocation.getGeometry());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      log.error("Json Exception converting json to string", e);
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

  public List<LocationRelationship> getLocationRelationshipsForLocationHierarchy(
      LocationHierarchy locationHierarchy) {
    return locationRelationshipRepository.findByLocationHierarchyIdentifier(
        locationHierarchy.getIdentifier()).stream().map((LocationRelationship::new)).collect(
        Collectors.toList());
  }

  public List<LocationRelationshipProjection> getLocationRelationshipsForLocationHierarchyId(
      String locationHierarchy) {
    return locationRelationshipRepository.findByLocationHierarchyIdentifier(
        UUID.fromString(locationHierarchy));
  }

  public List<LocationRelationshipProjection> getLocationRelationshipsNotLike(
      LocationHierarchy locationHierarchy, List<String> notLike) {
    return locationRelationshipRepository.findByLocationHierarchyWithoutStructuresNotLike(notLike);
  }

  public List<LocationRelationshipProjection> getLocationRelationshipsWithoutStructure(
      LocationHierarchy locationHierarchy) {
    return locationRelationshipRepository.findByLocationHierarchyWithoutStructures(
        locationHierarchy.getIdentifier());
  }

  public List<LocationChildrenCountProjection> getLocationChildrenCount(
      UUID locationHierarchyIdentifier) {
    return locationRelationshipRepository.getLocationChildrenCount(locationHierarchyIdentifier);
  }

  public List<LocationChildrenCountProjection> getLocationAssignedChildrenCount(
      UUID locationHierarchyIdentifier, UUID planIdentifier) {
    return locationRelationshipRepository.getLocationAssignedChildrenCount(
        locationHierarchyIdentifier, planIdentifier);
  }

  public long getNumberOfChildren(UUID locationIdentifier) {
    Optional<Long> response = locationRelationshipRepository.getChildrenNumber(locationIdentifier);
    if (response.isPresent()) {
      return response.get();
    }
    return 0;
  }


  public List<PlanLocationDetails> getLocationChildrenByLocationParentIdentifierAndPlanIdentifier(
      UUID parentLocationIdentifiers, UUID planIdentifier) {

    return locationRelationshipRepository.getLocationDetailsByParentIdAndPlanId(
        parentLocationIdentifiers, planIdentifier);
  }

  public List<PlanLocationDetails> getAssignedLocationChildrenByLocationParentIdentifierAndPlanIdentifier(
      UUID parentLocationIdentifiers, UUID planIdentifier) {
    return locationRelationshipRepository.getAssignedLocationDetailsByParentIdAndPlanId(
        parentLocationIdentifiers, planIdentifier);
  }

  public PlanLocationDetails getRootLocationDetailsByPlanId(UUID planIdentifier) {
    return locationRelationshipRepository.getRootLocationDetailsByAndPlanId(planIdentifier);
  }


  public Location findParentLocationByLocationIdAndHierarchyId(UUID locationIdentifier,
      UUID hierarchyIdentifier) {
    return locationRelationshipRepository.getParentLocationByLocationIdAndHierarchyId(
        locationIdentifier, hierarchyIdentifier);
  }



  public LocationRelationship getLocationRelationshipsForLocation(
      UUID locationHierarchyIdentifier, UUID locationIdentifier) {
    return locationRelationshipRepository.getFirstLocationRelationshipByLocation_IdentifierAndLocationHierarchy_Identifier(
        locationIdentifier, locationHierarchyIdentifier);
  }

  public Map<UUID, Location> getLocationRelationshipsForLocations(
      UUID locationHierarchyIdentifier, List<UUID> locationIdentifiers) {
    return locationRelationshipRepository.getLocationRelationshipByLocation_IdentifierInAndLocationHierarchy_Identifier(
        locationIdentifiers, locationHierarchyIdentifier).stream().collect(
        Collectors.toMap(locationRelationship -> locationRelationship.getLocation().getIdentifier(),
            locationRelationship ->
                locationRelationship.getParentLocation() == null ? new Location()
                    : locationRelationship.getParentLocation(), (a, b) -> b));

  }

  @Async
  public void refreshLocationCountsView() {
    locationCountsRepository.refreshLocationCountsMaterializedView();
  }

  @Async
  public void refreshLocationRelationshipMaterializedView() {
    locationRelationshipRepository.refreshLocationRelationshipMaterializedView();
  }

  @Async
  public void refreshLiteStructureCountView() {
    liteStructureCountRepository.refreshLiteStructureCount();
  }

  public List<Location> getChildrenLocations(UUID hierarchyIdentifier, UUID locationIdentifier) {
    List<Location> children = locationRelationshipRepository.getChildren(hierarchyIdentifier,
        locationIdentifier);
    return children;
  }


  public Set<Location> getStructuresForPlanIfHierarchyHasStructure(
      LocationHierarchy locationHierarchy, List<Pair<UUID, String>> planLocations) {

    // 1. check if hierarchy has structures
    // 2. get the parent of structure from the hierarchy
    // 3. get children locations of the locations that has that parent node name
    // 4. If structure is not in hierarchy then get children of the lowest location level in hierarchy

    Set<Location> structureLocations = new HashSet<>();

    if (locationHierarchy.getNodeOrder().contains(STRUCTURE)) {
      Set<Location> structures = planLocations.stream()
          .filter(planLocation -> planLocation.getSecond().equals(
              locationHierarchy.getNodeOrder()
                  .get(locationHierarchy.getNodeOrder().indexOf(STRUCTURE) - 1))
          )
          .flatMap(filteredPlanLocation -> getChildrenLocations(
              locationHierarchy.getIdentifier(), filteredPlanLocation.getFirst())
              .stream()).collect(Collectors.toSet());
      structureLocations.addAll(structures);
    } else {
      Set<Location> structures = planLocations.stream()
          .filter(planLocation -> planLocation.getSecond().equals(
              locationHierarchy.getNodeOrder()
                  .get(locationHierarchy.getNodeOrder().size() - 1))
          ).flatMap(filteredPlanLocation -> getChildrenLocations(
              locationHierarchy.getIdentifier(), filteredPlanLocation.getFirst())
              .stream()).collect(Collectors.toSet());
      structureLocations.addAll(structures);
    }

    return structureLocations;
  }

  public Location getLocationParent(UUID locationIdentifier, UUID locationHierarchyIdentifier) {
    return locationRelationshipRepository.getParentLocationByLocationIdAndHierarchyId(
        locationIdentifier, locationHierarchyIdentifier);
  }


  public List<LocationMainData> getLocationsByHierarchyIdAndLevelName(UUID hierarchyIdentifier,
      String levelName) {
    return locationRelationshipRepository.getLocationsByHierarchyIdAndLevelName(hierarchyIdentifier,
        levelName);
  }

  public List<UUID> getAncestryForLocation(UUID locationId, UUID locationHierarchyId)
      throws NotFoundException {

    List<LocationRelationship> locationRelationships = locationRelationshipRepository.findLocationRelationshipsByLocation_IdentifierAndLocationHierarchy_Identifier(
        locationId, locationHierarchyId);

    if (locationRelationships != null && !locationRelationships.isEmpty()) {
      LocationRelationship locationRelationship = locationRelationships.get(0);
      if (locationRelationship.getAncestry() != null) {
        return locationRelationship.getAncestry();
      }
    }
    throw new NotFoundException("No Ancestry for location id " + locationId + " hierarchy " +
        locationHierarchyId);
  }

  public List<UUID> getChildrenLocationIdentifiersGivenParentIdentifiers(
      List<UUID> parentIdentifiers) {
    return locationRelationshipRepository.getDistinctChildrenLocationsGivenParentIds(
        parentIdentifiers);
  }

  public void createLocationRelationship(Location parentLocation, Location childLocation,
      LocationHierarchy hierarchy) {
    if (hierarchy == null) {
      return;
    }
    List<UUID> ancestry = new ArrayList<>();
    ancestry.add(parentLocation.getIdentifier());
    ancestry.addAll(
        locationRelationshipRepository.findByLocationHierarchyIdentifierAndLocationIdentifier(
            hierarchy.getIdentifier(), parentLocation.getIdentifier()).get().getAncestry());
    LocationRelationship locationRelationship = LocationRelationship.builder()
        .parentLocation(parentLocation)
        .location(childLocation)
        .locationHierarchy(hierarchy).ancestry(ancestry).build();
    locationRelationship.setEntityStatus(EntityStatus.ACTIVE);
    locationRelationshipRepository.save(
        locationRelationship);
  }

  public LocationAndHigherParentProjection getHigherLocationParentByLocationAndParentGeographicLevelType(
      UUID locationIdentifier,
      UUID locationHierarchyIdentifier, String parentGeographicLevelName) {
    return locationRelationshipRepository.getHigherLocationParentByLocationAndParentGeographicLevelType(
        locationIdentifier, locationHierarchyIdentifier, parentGeographicLevelName);
  }
}

