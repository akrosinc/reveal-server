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

  @Autowired
  public PlanLocationsService(PlanLocationsRepository planLocationsRepository,
      @Lazy PlanService planService, LocationService locationService,
      LocationHierarchyService locationHierarchyService,
      @Lazy PlanAssignmentService planAssignmentService) {
    this.planLocationsRepository = planLocationsRepository;
    this.planService = planService;
    this.locationService = locationService;
    this.locationHierarchyService = locationHierarchyService;
    this.planAssignmentService = planAssignmentService;
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

  public void selectPlanLocations(UUID planIdentifier, Set<UUID> locations) {
    List<PlanLocations> planLocations = getPlanLocationsByPlanIdentifier(planIdentifier);
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    planLocations.forEach(el -> {
      if (!locations.contains(el.getLocation().getIdentifier())) {
        planLocationsRepository.delete(el);
      } else {
        locations.remove(el.getLocation().getIdentifier());
      }
    });
    locations.forEach(el -> {
      planLocationsRepository.save(new PlanLocations(plan, locationService.findByIdentifier(el)));
    });
  }

  public List<GeoTreeResponse> getHierarchyByPlanIdentifier(UUID identifier) {
    Plan plan = planService.getPlanByIdentifier((identifier));
    LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(
        plan.getLocationHierarchy().getIdentifier());
    List<GeoTreeResponse> geoTreeResponses = LocationHierarchyResponseFactory.generateGeoTreeResponseFromTree(
        locationHierarchyService.getGeoTreeFromLocationHierarchyWithoutStructure(locationHierarchy)
            .getLocationsHierarchy(), false);
    Set<Location> locations = plan.getPlanLocations().stream().map(PlanLocations::getLocation)
        .collect(
            Collectors.toSet());
    Map<UUID, Location> locationMap = locations.stream()
        .collect(Collectors.toMap(Location::getIdentifier, location -> location));
    geoTreeResponses.forEach(el -> {
      assignLocations(locationMap, el, plan.getIdentifier());
    });
    return geoTreeResponses;
  }

  public void assignLocations(Map<UUID, Location> locationMap, GeoTreeResponse geoTreeResponse,
      UUID planId) {
    List<PlanAssignment> planAssignments = planAssignmentService.getPlanAssignmentByPlanLocationIdentifier(
        planId, geoTreeResponse.getIdentifier());
    List<OrganizationResponse> teams = planAssignments.stream()
        .map(el -> OrganizationResponseFactory.fromEntityWithoutChild(el.getOrganization()))
        .collect(Collectors.toList());
    geoTreeResponse.setTeams(teams);
    if (locationMap.containsKey(geoTreeResponse.getIdentifier())) {
      geoTreeResponse.setActive(true);
    } else {
      geoTreeResponse.setActive(false);
    }
    geoTreeResponse.getChildren().forEach(el -> assignLocations(locationMap, el, planId));
  }
}