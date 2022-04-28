package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationHierarchyResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.OrganizationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.OrganizationResponse;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import com.revealprecision.revealserver.persistence.repository.PlanLocationsRepository;
import com.revealprecision.revealserver.persistence.repository.PlanRepository;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;


@Service
public class PlanLocationsService {

  private final PlanLocationsRepository planLocationsRepository;
  private final PlanService planService;
  private final LocationService locationService;
  private final LocationHierarchyService locationHierarchyService;
  private final PlanAssignmentService planAssignmentService;
  private final PlanRepository planRepository;

  @Autowired
  public PlanLocationsService(PlanLocationsRepository planLocationsRepository,
      @Lazy PlanService planService, LocationService locationService,
      LocationHierarchyService locationHierarchyService,
      @Lazy PlanAssignmentService planAssignmentService,
      PlanRepository planRepository) {
    this.planLocationsRepository = planLocationsRepository;
    this.planService = planService;
    this.locationService = locationService;
    this.locationHierarchyService = locationHierarchyService;
    this.planAssignmentService = planAssignmentService;
    this.planRepository = planRepository;
  }

  public List<PlanLocations> getPlanLocationsByPlanIdentifier(UUID planIdentifier) {
    return planLocationsRepository.findByPlan_Identifier(planIdentifier);
  }

  public List<PlanLocations> getPlanLocationsByLocationIdentifier(UUID locationIdentifier) {
    return planLocationsRepository.findByLocation_Identifier(locationIdentifier);
  }

  public List<PlanLocations> getPlanLocationsByLocationIdentifierList(
      List<UUID> locationIdentifiers) {
    return planLocationsRepository.findByLocation_IdentifierIn(locationIdentifiers);
  }

  public PlanLocations getPlanLocationByPlanIdentifierAndLocationIdentifier(UUID planId,
      UUID locationId) {
    return planLocationsRepository.findByPlan_IdentifierAndLocation_Identifier(planId, locationId);
  }

  public List<PlanLocations> getByPlanIdAndLocationIdentifiers(UUID planIdentifier, List<UUID> locationIdentifiers) {
    return planLocationsRepository.findByPlan_IdentifierAndLocation_IdentifierIn(planIdentifier, locationIdentifiers);
  }

  public void assignLocation(UUID planIdentifier, UUID locationIdentifier) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    Location location = locationService.findByIdentifier(locationIdentifier);
    planLocationsRepository.save(new PlanLocations(plan, location));
  }

  public void selectPlanLocations(UUID planIdentifier, Set<UUID> locations) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    if (locations.size() == 0) {
      plan.getPlanLocations().clear();
      planRepository.save(plan);
    } else {

      Set<UUID> currentLocation = planLocationsRepository.findByPlan_Identifier(planIdentifier).stream()
          .map(planLocations1 -> planLocations1.getLocation().getIdentifier()).collect(
              Collectors.toSet());

      Set<UUID> locationsToAdd = new HashSet<>(locations);
      locationsToAdd.removeAll(currentLocation);
      currentLocation.removeAll(locations);

      List<Location> addLocations = locationService.getAllByIdentifiers(
          new ArrayList<>(locationsToAdd));
      List<PlanLocations> addPlanLocations = addLocations.stream()
          .map(location -> new PlanLocations(plan, location))
          .collect(Collectors.toList());

      if (addPlanLocations.size() > 0) {
        planLocationsRepository.saveAll(addPlanLocations);
      }
      if (currentLocation.size() > 0) {
        planLocationsRepository.deletePlanLocationsByPlanAndLocation(planIdentifier,
            new ArrayList<>(currentLocation));
      }
    }
  }

  public List<GeoTreeResponse> getHierarchyByPlanIdentifier(UUID identifier) {
    Plan plan = planService.getPlanByIdentifier((identifier));
    LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(
        plan.getLocationHierarchy().getIdentifier());
    List<GeoTreeResponse> geoTreeResponses = LocationHierarchyResponseFactory.generateGeoTreeResponseFromTree(
        locationHierarchyService.getGeoTreeFromLocationHierarchyWithoutStructure(locationHierarchy)
            .getLocationsHierarchy(), false);
    System.out.println("geotree gotov " + new Date());
    Set<Location> locations = planLocationsRepository.findLocationsByPlan_Identifier(plan.getIdentifier());
    System.out.println("set lokacija gotov" + new Date());
    Map<UUID, Location> locationMap = locations.stream()
        .collect(Collectors.toMap(Location::getIdentifier, location -> location));
    System.out.println("mapa lokacija gotova " + new Date());
    List<PlanAssignment> planAssignments = planAssignmentService.getPlanAssignmentsByPlanIdentifier(
        identifier);
    System.out.println("plan assignmenti gotovi " + new Date());
    Map<UUID, List<PlanAssignment>> planAssignmentMap = planAssignments.stream()
        .collect(Collectors.groupingBy(planAssignment -> planAssignment.getPlanLocations().getLocation().getIdentifier()));
    System.out.println("pre rekurzije " + new Date());
    geoTreeResponses.forEach(el -> {
      assignLocations(locationMap, el, planAssignmentMap);
    });
    System.out.println("posle rekurzije " + new Date());
    return geoTreeResponses;
  }

  public void assignLocations(Map<UUID, Location> locationMap, GeoTreeResponse geoTreeResponse,  Map<UUID, List<PlanAssignment>> planAssignmentMap) {
    List<PlanAssignment> planAssignments = planAssignmentMap.get(geoTreeResponse.getIdentifier());
    if(planAssignments == null) {
      geoTreeResponse.setTeams(new ArrayList<>());
    }else {
      List<OrganizationResponse> teams = planAssignments.stream()
          .map(el -> OrganizationResponseFactory.fromEntityIdAndName(el.getOrganization()))
          .collect(Collectors.toList());
      geoTreeResponse.setTeams(teams);
    }
    if (locationMap.containsKey(geoTreeResponse.getIdentifier())) {
      geoTreeResponse.setActive(true);
    } else {
      geoTreeResponse.setActive(false);
    }
    geoTreeResponse.getChildren().forEach(el -> assignLocations(locationMap, el, planAssignmentMap));
  }

  public Long getPlanLocationsCount(UUID planIdentifier) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    return planLocationsRepository.countByPlan_Identifier(plan.getIdentifier());
  }

  public Set<PlanLocations> getPlanLocationsByPlanIdAndLocationIds(UUID planId, List<UUID> locationIds) {
    return planLocationsRepository.getPlanLocationsByPlanIdAndLocationIdentifiers(planId, locationIds);
  }
}