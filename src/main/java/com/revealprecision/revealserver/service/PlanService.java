package com.revealprecision.revealserver.service;

import com.cosium.spring.data.jpa.entity.graph.domain.EntityGraphUtils;
import com.revealprecision.revealserver.api.v1.dto.factory.PlanEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.PlanRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.message.Message;
import com.revealprecision.revealserver.messaging.message.PlanCreateMessage;
import com.revealprecision.revealserver.messaging.message.PlanUpdateMessage;
import com.revealprecision.revealserver.messaging.message.PlanUpdateType;
import com.revealprecision.revealserver.messaging.TopicConstants;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Condition;
import com.revealprecision.revealserver.persistence.domain.Form;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import com.revealprecision.revealserver.persistence.domain.LookupInterventionType;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Plan.Fields;
import com.revealprecision.revealserver.persistence.repository.PlanRepository;
import com.revealprecision.revealserver.util.UserUtils;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class PlanService {

  private final PlanRepository planRepository;
  private final FormService formService;
  private final LocationHierarchyService locationHierarchyService;
  private final LookupInterventionTypeService lookupInterventionTypeService;
  private final TaskService taskService;
  private final LookupEntityTypeService lookupEntityTypeService;
  private final KafkaTemplate<String, Message> kafkaTemplate;

  public static boolean isNullOrEmpty(final Collection<?> c) {
    return c == null || c.isEmpty();
  }

  public Plan getPlanByIdentifier(UUID planIdentifier) {
    return planRepository.findById(planIdentifier).orElseThrow(
        () -> new NotFoundException(Pair.of(Fields.identifier, planIdentifier), Plan.class));
  }


  public List<Location> findLocationsForPlan(UUID planIdentifier) {
    return planRepository.findLocationsForPlan(planIdentifier);
  }

  public Page<Plan> getAll(String search, Pageable pageable) {
    return planRepository.getAll(search, pageable, EntityGraphUtils.fromAttributePaths(
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
    List<LookupEntityType> allLookUpEntityTypes = lookupEntityTypeService.getAllLookUpEntityTypes();
    if (!isNullOrEmpty(planRequest.getGoals())) {
      planRequest.getGoals().forEach(goalRequest -> {
        if (!isNullOrEmpty(goalRequest.getActions())) {
          goalRequest.getActions()
              .forEach(actionRequest -> forms.add(actionRequest.getFormIdentifier()));
        }
      });
    }

    Map<UUID, Form> foundForms = formService.findByIdentifiers(forms);

    Plan plan = PlanEntityFactory.toEntity(planRequest, interventionType, locationHierarchy,
        foundForms, allLookUpEntityTypes);
    plan.setEntityStatus(EntityStatus.ACTIVE);

    savePlan(plan);
    PlanCreateMessage planCreateMessage = new PlanCreateMessage();
    planCreateMessage.setUserId(UserUtils.getCurrentPrinciple().getName());
    log.info("userid {}", UserUtils.getCurrentPrinciple().getName());
    planCreateMessage.setPlanIdentifier(plan.getIdentifier());
    kafkaTemplate.send(TopicConstants.PLAN_CREATE,planCreateMessage);
  }

  public void activatePlan(UUID planIdentifier) {
    Plan plan = getPlanByIdentifier(planIdentifier);
    plan.setStatus(PlanStatusEnum.ACTIVE);
    savePlan(plan);
    PlanUpdateMessage planUpdateMessage = new PlanUpdateMessage();
    planUpdateMessage.setPlanIdentifier(plan.getIdentifier());
    planUpdateMessage.setPlanUpdateType(PlanUpdateType.ACTIVATE);

    kafkaTemplate.send(TopicConstants.PLAN_UPDATE,planUpdateMessage);
  }

  public void updatePlan(PlanRequest request, UUID identifier) {
    Plan plan = getPlanByIdentifier(identifier);
    LocationHierarchy hierarchy = locationHierarchyService.findByIdentifier(
        request.getLocationHierarchy());
    LookupInterventionType interventionType = lookupInterventionTypeService.findByIdentifier(
        request.getInterventionType());
    plan.update(request, hierarchy, interventionType);
    savePlan(plan);
  }

  private void savePlan(Plan plan) {
    Plan savedPlan = planRepository.save(plan);
    generateAndUpdateTasks(savedPlan.getIdentifier());
  }

  private void generateAndUpdateTasks(UUID planIdentifier) {
    taskService.generateTasksByPlanId(planIdentifier);
  }

}