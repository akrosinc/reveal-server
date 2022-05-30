package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.OrganizationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.OrganizationResponse;
import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.PlanLocationAssignMessage;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import com.revealprecision.revealserver.persistence.repository.PlanLocationsRepository;
import com.revealprecision.revealserver.persistence.repository.PlanRepository;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.util.UserUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class PlanLocationsService {

  private final PlanLocationsRepository planLocationsRepository;
  private final PlanService planService;
  private final LocationService locationService;
  private final LocationHierarchyService locationHierarchyService;
  private final PlanAssignmentService planAssignmentService;
  private final PlanRepository planRepository;
  private final KafkaTemplate<String, PlanLocationAssignMessage> kafkaTemplate;
  private final KafkaProperties kafkaProperties;

  @Autowired
  public PlanLocationsService(PlanLocationsRepository planLocationsRepository,
      @Lazy PlanService planService, LocationService locationService,
      LocationHierarchyService locationHierarchyService,
      @Lazy PlanAssignmentService planAssignmentService,
      PlanRepository planRepository,
      KafkaTemplate<String, PlanLocationAssignMessage> kafkaTemplate,
      KafkaProperties kafkaProperties) {
    this.planLocationsRepository = planLocationsRepository;
    this.planService = planService;
    this.locationService = locationService;
    this.locationHierarchyService = locationHierarchyService;
    this.planAssignmentService = planAssignmentService;
    this.planRepository = planRepository;
    this.kafkaTemplate = kafkaTemplate;
    this.kafkaProperties=kafkaProperties;

  }

  public List<PlanLocations> getPlanLocationsByPlanIdentifier(UUID planIdentifier) {
    return planLocationsRepository.findByPlan_Identifier(planIdentifier);
  }

  public List<PlanLocations> getPlanLocationsByPlanAndLocationIdentifier(UUID locationIdentifier,
      UUID planIdentifier) {
    return planLocationsRepository.findByLocation_IdentifierAndPlan_Identifier(locationIdentifier, planIdentifier);
  }

  public List<PlanLocations> getPlanLocationsByLocationIdentifierList(
      List<UUID> locationIdentifiers) {
    return planLocationsRepository.findByLocation_IdentifierIn(locationIdentifiers);
  }

  public PlanLocations getPlanLocationByPlanIdentifierAndLocationIdentifier(UUID planId,
      UUID locationId) {
    return planLocationsRepository.findByPlan_IdentifierAndLocation_Identifier(planId, locationId);
  }

  public List<PlanLocations> getByPlanIdAndLocationIdentifiers(UUID planIdentifier,
      List<UUID> locationIdentifiers) {
    return planLocationsRepository.findByPlan_IdentifierAndLocation_IdentifierIn(planIdentifier,
        locationIdentifiers);
  }

  public void assignLocation(UUID planIdentifier, UUID locationIdentifier) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    Location location = locationService.findByIdentifier(locationIdentifier);

    List<UUID> locationsToAdd = locationService.getAllLocationChildren(locationIdentifier, plan.getLocationHierarchy().getIdentifier());
    locationsToAdd.add(locationIdentifier);
    List<PlanLocations> addPlanLocations = locationsToAdd.stream()
        .map(loc -> new PlanLocations(plan, loc))
        .collect(Collectors.toList());
    planLocationsRepository.saveAll(addPlanLocations);
  }

  public void selectPlanLocations(UUID planIdentifier, Set<UUID> locations) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);

    PlanLocationAssignMessage planLocationAssignMessage = new PlanLocationAssignMessage();
    planLocationAssignMessage.setPlanIdentifier(planIdentifier.toString());
    planLocationAssignMessage.setLocationsAdded(
        locations.stream()
            .map(UUID::toString)
            .collect(Collectors.toList())
    );
    planLocationAssignMessage.setOwnerId(UserUtils.getCurrentPrincipleName());

    List<UUID> removedPlanLocations = plan.getPlanLocations().stream().map(PlanLocations::getLocation)
        .map(Location::getIdentifier).collect(Collectors.toList());
    removedPlanLocations.removeAll(locations);
    List<String> removedPlanLocationsString = removedPlanLocations.stream().map(UUID::toString)
        .collect(Collectors.toList());
    planLocationAssignMessage.setLocationsRemoved(removedPlanLocationsString);

    if (locations.size() == 0) {
      planLocationsRepository.deleteByPlanIdentifier(planIdentifier);
      kafkaTemplate.send(kafkaProperties.getTopicMap().get(KafkaConstants.PLAN_LOCATION_ASSIGNED), planLocationAssignMessage);
    } else {
      Set<UUID> currentLocation = planLocationsRepository.findByPlan_Identifier(planIdentifier).stream()
          .map(planLocations1 -> planLocations1.getLocation().getIdentifier()).collect(
              Collectors.toSet());
      Set<UUID> locationsToAdd = new HashSet<>(locations);
      locationsToAdd.removeAll(currentLocation);
      currentLocation.removeAll(locations);

      List<PlanLocations> addPlanLocations = locationsToAdd.stream()
          .map(location -> new PlanLocations(plan, location))
          .collect(Collectors.toList());

      if (addPlanLocations.size() > 0) {
        planLocationsRepository.saveAll(addPlanLocations);
      }
      if (currentLocation.size() > 0) {
        planLocationsRepository.deletePlanLocationsByPlanAndLocation(planIdentifier,
            new ArrayList<>(currentLocation));
      }

      kafkaTemplate.send(kafkaProperties.getTopicMap().get(KafkaConstants.PLAN_LOCATION_ASSIGNED), planLocationAssignMessage);
      log.info("sent plan location");
    }
  }

  public List<GeoTreeResponse> getHierarchyByPlanIdentifier(UUID identifier) {
    Plan plan = planService.getPlanByIdentifier((identifier));
    LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(
        plan.getLocationHierarchy().getIdentifier());

    List<GeoTreeResponse> geoTreeResponses = locationHierarchyService.getGeoTreeFromLocationHierarchyWithoutStructure(locationHierarchy);
    Set<Location> locations = planLocationsRepository.findLocationsByPlan_Identifier(plan.getIdentifier());
    Map<UUID, Location> locationMap = locations.stream()
        .collect(Collectors.toMap(Location::getIdentifier, location -> location));
    List<PlanAssignment> planAssignments = planAssignmentService.getPlanAssignmentsByPlanIdentifier(
        identifier);
    Map<UUID, List<PlanAssignment>> planAssignmentMap = planAssignments.stream()
        .collect(Collectors.groupingBy(
            planAssignment -> planAssignment.getPlanLocations().getLocation().getIdentifier()));
    geoTreeResponses.forEach(el -> {
      assignLocations(locationMap, el, planAssignmentMap);
    });
    return geoTreeResponses;
  }

  public void assignLocations(Map<UUID, Location> locationMap, GeoTreeResponse geoTreeResponse,
      Map<UUID, List<PlanAssignment>> planAssignmentMap) {
    List<PlanAssignment> planAssignments = planAssignmentMap.get(geoTreeResponse.getIdentifier());
    if (planAssignments == null) {
      geoTreeResponse.setTeams(new ArrayList<>());
    } else {
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
    if(geoTreeResponse.getChildren() != null) {
      geoTreeResponse.getChildren().forEach(el -> assignLocations(locationMap, el, planAssignmentMap));
    }
  }

  public Long getPlanLocationsCount(UUID planIdentifier) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    return planLocationsRepository.countByPlan_Identifier(plan.getIdentifier());
  }

  public Set<PlanLocations> getPlanLocationsByPlanIdAndLocationIds(UUID planId, List<UUID> locationIds) {
    return planLocationsRepository.getPlanLocationsByPlanIdAndLocationIdentifiers(planId, locationIds);
  }
}