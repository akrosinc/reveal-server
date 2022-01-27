package com.revealprecision.revealserver.service;

import com.cosium.spring.data.jpa.entity.graph.domain.EntityGraphUtils;
import com.revealprecision.revealserver.api.v1.dto.factory.PlanEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.PlanRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Form;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LookupInterventionType;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Plan.Fields;
import com.revealprecision.revealserver.persistence.repository.PlanRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
            String.join(".", Fields.goals, Goal.Fields.actions),
            String.join(".", Fields.goals, Goal.Fields.targets)));
  }

  public long getAllCount(String search) {
    return planRepository.getAllCount(search);
  }

  public void createPlan(PlanRequest planRequest) {

    Set<Location> locations = null;
    if (!isNullOrEmpty(planRequest.getJurisdictions())) {
      List<Location> foundLocations = locationService.getAllByIdentifiers(
          new ArrayList<>(planRequest.getJurisdictions()));
      locations = new HashSet<>(foundLocations);
    }
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
    plan.setLocations(locations);
    plan.setEntityStatus(EntityStatus.ACTIVE);
    planRepository.save(plan);

  }

  public void activatePlan(UUID planIdentifier) {
    Plan plan = getPlanByIdentifier(planIdentifier);
    boolean valid = true;
    if (isNullOrEmpty(plan.getGoals())) {
      valid = false;
    } else {
      for (Goal goal : plan.getGoals()) {
        if (isNullOrEmpty(goal.getActions())) {
          valid = false;
        }
      }
    }

    if (!((LocalDate.now().isAfter(plan.getEffectivePeriodStart()) || LocalDate.now()
        .isEqual(plan.getEffectivePeriodStart())) && (
        LocalDate.now().isBefore(plan.getEffectivePeriodEnd()) || LocalDate.now()
            .isEqual(plan.getEffectivePeriodEnd())))) {
      valid = false;
    }
    if (valid) {
      plan.setStatus(PlanStatusEnum.ACTIVE);
      planRepository.save(plan);
    } else {
      throw new ConflictException("Plan could not be activated");
    }
  }
}