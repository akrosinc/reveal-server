package com.revealprecision.revealserver.service;

import com.cosium.spring.data.jpa.entity.graph.domain.EntityGraphUtils;
import com.revealprecision.revealserver.api.v1.dto.factory.PlanEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.ActionRequest;
import com.revealprecision.revealserver.api.v1.dto.request.Context;
import com.revealprecision.revealserver.api.v1.dto.request.PlanRequest;
import com.revealprecision.revealserver.batch.BatchConstants.Constraint;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.LookupUtil;
import com.revealprecision.revealserver.enums.PlanInterventionTypeEnum;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.constant.Error;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Plan.Fields;
import com.revealprecision.revealserver.persistence.repository.FormRepository;
import com.revealprecision.revealserver.persistence.repository.PlanRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
  private final FormRepository formRepository;
  private final LocationHierarchyService locationHierarchyService;
  private final LocationRelationshipService locationRelationshipService;
  private final LocationService locationService;

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
    if (planRequest.getActions() != null) {
      checkForms(planRequest);
    }
    var wrapper = new Object() {
      PlanInterventionTypeEnum type;
      UUID locationHierarchyId;
    };
    planRequest.getUseContext().forEach(context -> {
      switch (context.getCode()) {
        case "interventionType":
          wrapper.type = LookupUtil.lookup(PlanInterventionTypeEnum.class,
              context.getValueCodableConcept());
          break;
        case "locationHierarchy":
          if (context.getValueCodableConcept().matches(Constraint.UUID_REGEX)) {
            wrapper.locationHierarchyId = UUID.fromString(context.getValueCodableConcept());
          } else {
            throw new ConflictException(
                String.format(Error.WRONG_FORMAT, Context.Fields.valueCodableConcept,
                    context.getValueCodableConcept(), UUID.class.getSimpleName()));
          }
          break;
      }
    });
    if (wrapper.locationHierarchyId == null) {
      throw new ConflictException("Location hierarchy must not be null");
    }
    LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(
        wrapper.locationHierarchyId);
    Set<Location> locations = null;
    if (!isNullOrEmpty(planRequest.getJurisdictions())) {
      Set<UUID> lookForLocations = new HashSet<>();
      planRequest.getJurisdictions().stream()
          .forEach(jurisdiction -> lookForLocations.addAll(jurisdiction.getCode()));
      locationRelationshipService.validateLocationsBelonging(locationHierarchy.getIdentifier(),
          lookForLocations);
      List<Location> foundLocations = locationService.getAllByIdentifiers(
          new ArrayList<>(lookForLocations));
      locations = new HashSet<>(foundLocations);
    } else {
      throw new ConflictException("Jurisdiction must not be empty");
    }
    Plan plan = PlanEntityFactory.toEntity(planRequest, wrapper.type, locationHierarchy);
    plan.setLocations(locations);
    plan.setEntityStatus(EntityStatus.ACTIVE);
    planRepository.save(plan);

  }

  public void checkForms(PlanRequest planRequest) {
    var forms = planRequest.getActions().stream()
        .map(ActionRequest::getDefinitionUri).collect(
            Collectors.toList());

    //TODO call service
    List<String> foundForms = formRepository.getFormsByName(forms);

    forms.removeAll(foundForms);
    if (forms.size() > 0) {
      throw new NotFoundException("Forms: " + forms + " not found");
    }
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