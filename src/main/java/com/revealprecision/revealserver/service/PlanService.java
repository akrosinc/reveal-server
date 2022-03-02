package com.revealprecision.revealserver.service;

import com.cosium.spring.data.jpa.entity.graph.domain.EntityGraphUtils;
import com.revealprecision.revealserver.api.v1.dto.factory.LocationHierarchyResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.PlanEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.PlanRequest;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTree;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.*;
import com.revealprecision.revealserver.persistence.domain.Plan.Fields;
import com.revealprecision.revealserver.persistence.repository.PlanRepository;

import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PlanService {

  private final PlanRepository planRepository;
  private final FormService formService;
  private final LocationHierarchyService locationHierarchyService;
  private final LocationRelationshipService locationRelationshipService;
  private final LocationService locationService;
  private final LookupInterventionTypeService lookupInterventionTypeService;

  public static boolean isNullOrEmpty(final Collection<?> c) {
    return c == null || c.isEmpty();
  }

  public Plan getPlanByIdentifier(UUID planIdentifier) {
    return planRepository.findById(planIdentifier).orElseThrow(() ->
        new NotFoundException(Pair.of(Fields.identifier, planIdentifier), Plan.class));
  }

  public Page<Plan> getAll(String search, Pageable pageable) {
    return planRepository.getAll(search, pageable,
        EntityGraphUtils.fromAttributePaths(
            String.join(".", Fields.goals, Goal.Fields.actions, Action.Fields.conditions,
                Condition.Fields.targets)));
  }

  public long getAllCount(String search) {
    return planRepository.getAllCount(search);
  }

  public void createPlan(PlanRequest planRequest) {

    LookupInterventionType interventionType = lookupInterventionTypeService.findByIdentifier(
        planRequest.getInterventionType());
    LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(
        planRequest.getLocationHierarchy());

    Set<UUID> forms = new HashSet<>();
    if (!isNullOrEmpty(planRequest.getGoals())) {
      planRequest.getGoals().forEach(goalRequest -> {
        if (!isNullOrEmpty(goalRequest.getActions())) {
          goalRequest.getActions().forEach(actionRequest -> {
            forms.add(actionRequest.getFormIdentifier());
          });
        }
      });
    }

    Map<UUID, Form> foundForms = formService.findByIdentifiers(forms);

    Plan plan = PlanEntityFactory.toEntity(planRequest, interventionType, locationHierarchy,
        foundForms);
    plan.setEntityStatus(EntityStatus.ACTIVE);
    planRepository.save(plan);

  }

  public void activatePlan(UUID planIdentifier) {
    Plan plan = getPlanByIdentifier(planIdentifier);
    plan.setStatus(PlanStatusEnum.ACTIVE);
    planRepository.save(plan);
  }

  public void updatePlan(PlanRequest request, UUID identifier) {
    Plan plan = getPlanByIdentifier(identifier);
    LocationHierarchy hierarchy = locationHierarchyService.findByIdentifier(
        request.getLocationHierarchy());
    LookupInterventionType interventionType = lookupInterventionTypeService.findByIdentifier(
        request.getInterventionType());
    plan.update(request, hierarchy, interventionType);
    planRepository.save(plan);
  }

  public List<GeoTreeResponse> getHierarchyByPlanIdentifier(UUID identifier) {
    Plan plan = getPlanByIdentifier((identifier));
    LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(
        plan.getLocationHierarchy().getIdentifier());
    GeoTree geoTree = locationHierarchyService.getGeoTreeFromLocationHierarchy(locationHierarchy);
    List<GeoTreeResponse> geoTreeResponses = LocationHierarchyResponseFactory.generateGeoTreeResponseFromTree(
        locationHierarchyService.getGeoTreeFromLocationHierarchy(locationHierarchy)
            .getLocationsHierarchy(), false);
    Set<Location> locations = plan.getLocations().stream().map(PlanLocations::getLocation).collect(
        Collectors.toSet());
    Map<UUID, Location> locationMap = locations.stream()
        .collect(Collectors.toMap(Location::getIdentifier, location -> location));
    geoTreeResponses.forEach(el -> {
      assignLocations(locationMap, el);
    });
    return geoTreeResponses;
  }

  public Set<Location> getPlanLocationsByPlanId(UUID identifier) {
    Plan plan = getPlanByIdentifier((identifier));
    return plan.getLocations().stream().map(PlanLocations::getLocation).collect(Collectors.toSet());
  }

  public void assignLocations(Map<UUID, Location> locationMap, GeoTreeResponse geoTreeResponse) {
    if (locationMap.containsKey(geoTreeResponse.getIdentifier())) {
      geoTreeResponse.setActive(true);
    } else {
      geoTreeResponse.setActive(false);
    }
    geoTreeResponse.getChildren().forEach(el -> assignLocations(locationMap, el));
  }

  public void assignSelectedLocations(UUID identifier, Set<UUID> locations) {
    Plan plan = getPlanByIdentifier(identifier);
    Set<UUID> currentLocations = plan.getLocations().stream()
        .map(el -> el.getLocation().getIdentifier()).collect(Collectors.toSet());
    Set<UUID> locationsToAdd = new HashSet<>(locations);
    locationsToAdd.removeAll(currentLocations);
    locationsToAdd.forEach(el -> {
      plan.addLocation(new PlanLocations(plan, locationService.findByIdentifier((el))));
    });
    currentLocations.removeAll(locations);
    currentLocations.forEach(el -> {
      plan.removeLocation(
          plan.getLocations().stream().filter(planEl -> planEl.getLocation().getIdentifier() == el)
              .collect(Collectors.toList()).get(0));
    });
    planRepository.save(plan);
  }

  public void assignOrganizationsToLocation(Set<UUID> organizationIdentifiers,
      UUID locationIdentifier, UUID planId) {
    Set<Location> planLocations = getPlanLocationsByPlanId(planId);
  }
}