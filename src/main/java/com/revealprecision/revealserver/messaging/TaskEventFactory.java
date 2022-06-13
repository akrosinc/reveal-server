package com.revealprecision.revealserver.messaging;

import com.revealprecision.revealserver.messaging.message.ActionEvent;
import com.revealprecision.revealserver.messaging.message.GoalEvent;
import com.revealprecision.revealserver.messaging.message.LocationHierarchyEvent;
import com.revealprecision.revealserver.messaging.message.LookupEntityTypeEvent;
import com.revealprecision.revealserver.messaging.message.LookupInterventionTypeEvent;
import com.revealprecision.revealserver.messaging.message.LookupTaskStatusEvent;
import com.revealprecision.revealserver.messaging.message.PlanEvent;
import com.revealprecision.revealserver.messaging.message.TaskEvent;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import com.revealprecision.revealserver.persistence.domain.LookupInterventionType;
import com.revealprecision.revealserver.persistence.domain.LookupTaskStatus;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.util.ActionUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TaskEventFactory {

  public static TaskEvent getTaskEventFromTask(Task taskSaved){

    List<String> baseLocationIds = new ArrayList<>();

    Action action = taskSaved.getAction();
    if (ActionUtils.isActionForLocation(action)){
      baseLocationIds = List.of(taskSaved.getLocation().getIdentifier().toString());
    } else if (ActionUtils.isActionForLocation(action)){
      baseLocationIds = taskSaved.getPerson().getLocations().stream().map(Location::getIdentifier).map(
          UUID::toString).collect(
          Collectors.toList());
    }

    Goal goal = action.getGoal();
    Plan plan = goal.getPlan();
    LocationHierarchy locationHierarchy = plan
        .getLocationHierarchy();

    LookupInterventionType interventionType = plan
        .getInterventionType();
    LookupEntityType lookupEntityType = action.getLookupEntityType();

    LookupTaskStatus lookupTaskStatus = taskSaved.getLookupTaskStatus();


    TaskEvent taskEvent = TaskEvent.builder()
        .baseLocationIds(baseLocationIds)
        .action(getActionEventFromAction(action, goal, plan, locationHierarchy, interventionType,
            lookupEntityType))
        .executionPeriodStart(taskSaved.getExecutionPeriodStart())
        .executionPeriodEnd(taskSaved.getExecutionPeriodEnd())
        .businessStatus(taskSaved.getBusinessStatus())
        .authoredOn(taskSaved.getAuthoredOn())
        .baseEntityIdentifier(taskSaved.getBaseEntityIdentifier())
        .description(taskSaved.getDescription())
        .serverVersion(taskSaved.getServerVersion())
        .lookupTaskStatus(getLookupTaskStatusEventFromLookupTaskStatus(lookupTaskStatus))
        .lastModified(taskSaved.getLastModified())
        .identifier(taskSaved.getIdentifier())
        .priority(taskSaved.getPriority())
        .build();

    if (ActionUtils.isActionForPerson(taskSaved.getAction())){
      taskEvent.setPersonId(taskSaved.getPerson().getIdentifier().toString());
    }
    if (ActionUtils.isActionForLocation(taskSaved.getAction())){
      taskEvent.setLocationId(taskSaved.getLocation().getIdentifier().toString());
    }
    return taskEvent;
  }

  private static LookupTaskStatusEvent getLookupTaskStatusEventFromLookupTaskStatus(LookupTaskStatus lookupTaskStatus) {
    return LookupTaskStatusEvent.builder()
        .code(lookupTaskStatus.getCode())
        .identifier(lookupTaskStatus.getIdentifier())
        .name(lookupTaskStatus.getName())
        .build();
  }

  private static ActionEvent getActionEventFromAction(Action action, Goal goal, Plan plan,
      LocationHierarchy locationHierarchy, LookupInterventionType interventionType,
      LookupEntityType lookupEntityType) {
    return ActionEvent.builder()
        .goal(getGoalEventFromGoal(goal, plan, locationHierarchy, interventionType))
        .description(action.getDescription())
        .lookupEntityType(getLookupEntityTypeEventFromLookupEntityType(lookupEntityType))
        .timingPeriodEnd(action.getTimingPeriodEnd())
        .timingPeriodStart(action.getTimingPeriodStart())
        .type(action.getType())
        .identifier(action.getIdentifier())
        .title(action.getTitle())
        .build();
  }

  private static LookupEntityTypeEvent getLookupEntityTypeEventFromLookupEntityType(LookupEntityType lookupEntityType) {
    return LookupEntityTypeEvent.builder()
        .tableName(lookupEntityType.getTableName())
        .code(lookupEntityType.getCode())
        .identifier(lookupEntityType.getIdentifier())
        .build();
  }

  private static GoalEvent getGoalEventFromGoal(Goal goal, Plan plan, LocationHierarchy locationHierarchy,
      LookupInterventionType interventionType) {
    return GoalEvent.builder()
        .plan(getPlanEventFromPlan(plan, locationHierarchy, interventionType))
        .description(goal.getDescription())
        .identifier(goal.getIdentifier())
        .priority(goal.getPriority())
        .build();
  }

  private static PlanEvent getPlanEventFromPlan(Plan plan, LocationHierarchy locationHierarchy,
      LookupInterventionType interventionType) {
    return PlanEvent.builder()
        .locationHierarchy(getLocationHierarchyEventFromLocationHierarchy(locationHierarchy))
        .interventionType(getLookupInterventionTypeEventFromInterventionType(interventionType))
        .date(plan.getDate())
        .effectivePeriodEnd(
            plan.getEffectivePeriodEnd())
        .effectivePeriodStart(
            plan.getEffectivePeriodStart())
        .status(plan.getStatus())
        .title(plan.getTitle())
        .name(plan.getName())
        .identifier(plan.getIdentifier())
        .build();
  }

  private static LookupInterventionTypeEvent getLookupInterventionTypeEventFromInterventionType(LookupInterventionType interventionType) {
    return LookupInterventionTypeEvent.builder()
        .code(
            interventionType.getCode())
        .name(
            interventionType.getName())
        .identifier(interventionType
            .getIdentifier())
        .build();
  }

  private static LocationHierarchyEvent getLocationHierarchyEventFromLocationHierarchy(LocationHierarchy locationHierarchy) {
    return LocationHierarchyEvent.builder()
        .name(
            locationHierarchy.getName())
        .nodeOrder(locationHierarchy
            .getNodeOrder())
        .identifier(locationHierarchy
            .getIdentifier())
        .build();
  }

  public static TaskEvent copyTaskEvent(TaskEvent taskEventOriginal) {
    TaskEvent taskEvent = TaskEvent.builder()
        .baseLocationIds(taskEventOriginal.getBaseLocationIds())
        .locationName(taskEventOriginal.getLocationName())
        .action(getActionEvent(taskEventOriginal.getAction()))
        .businessStatus(taskEventOriginal.getBusinessStatus())
        .authoredOn(taskEventOriginal.getAuthoredOn())
        .baseEntityIdentifier(taskEventOriginal.getBaseEntityIdentifier())
        .executionPeriodStart(taskEventOriginal.getExecutionPeriodStart())
        .executionPeriodEnd(taskEventOriginal.getExecutionPeriodEnd())
        .description(taskEventOriginal.getDescription())
        .serverVersion(taskEventOriginal.getServerVersion())
        .lookupTaskStatus(getLookupTaskStatusEvent(taskEventOriginal.getLookupTaskStatus()))
        .lastModified(taskEventOriginal.getLastModified())
        .identifier(taskEventOriginal.getIdentifier())
        .priority(taskEventOriginal.getPriority())
        .owner(taskEventOriginal.getOwner())
        .ownerId(taskEventOriginal.getOwnerId())
        .build();
    if (taskEventOriginal.getLocationId()!=null){
      taskEvent.setLocationId(taskEventOriginal.getLocationId());
    }
    if (taskEventOriginal.getPersonId()!=null){
      taskEvent.setPersonId(taskEventOriginal.getPersonId());
    }
    return taskEvent;
  }

  private static LookupTaskStatusEvent getLookupTaskStatusEvent(LookupTaskStatusEvent lookupTaskStatusEvent) {
    return LookupTaskStatusEvent.builder()
        .code(lookupTaskStatusEvent.getCode())
        .identifier(lookupTaskStatusEvent.getIdentifier())
        .name(lookupTaskStatusEvent.getName())
        .build();
  }

  private static ActionEvent getActionEvent(ActionEvent actionEvent) {
    return ActionEvent.builder()
        .goal(getGoalEvent(actionEvent.getGoal()))
        .description(actionEvent.getDescription())
        .lookupEntityType(LookupEntityTypeEvent.builder()
            .tableName(actionEvent.getLookupEntityType().getTableName())
            .code(actionEvent.getLookupEntityType().getCode())
            .identifier(actionEvent.getLookupEntityType().getIdentifier())
            .build())
        .timingPeriodEnd(actionEvent.getTimingPeriodEnd())
        .timingPeriodStart(actionEvent.getTimingPeriodStart())
        .type(actionEvent.getType())
        .identifier(actionEvent.getIdentifier())
        .title(actionEvent.getTitle())
        .build();
  }

  private static GoalEvent getGoalEvent(GoalEvent goalEvent) {
    return GoalEvent.builder()
        .plan(getPlanEvent(goalEvent.getPlan()))
        .description(goalEvent.getDescription())
        .identifier(goalEvent.getIdentifier())
        .priority(goalEvent.getPriority())
        .build();
  }

  private static PlanEvent getPlanEvent(PlanEvent planEvent) {
    return PlanEvent.builder()
        .locationHierarchy(getLocationHierarchyEvent(planEvent.getLocationHierarchy()))
        .interventionType(getInterventionTypeEvent(planEvent.getInterventionType()))
        .date(planEvent.getDate())
        .effectivePeriodEnd(
            planEvent.getEffectivePeriodEnd())
        .effectivePeriodStart(
            planEvent.getEffectivePeriodStart())
        .status(planEvent.getStatus())
        .title(planEvent.getTitle())
        .name(planEvent.getName())
        .identifier(planEvent.getIdentifier())
        .build();
  }

  private static LookupInterventionTypeEvent getInterventionTypeEvent(LookupInterventionTypeEvent lookupInterventionTypeEvent) {
    return LookupInterventionTypeEvent.builder()
        .code(
            lookupInterventionTypeEvent
                .getCode())
        .name(
            lookupInterventionTypeEvent
                .getName())
        .identifier(
            lookupInterventionTypeEvent
                .getIdentifier())
        .build();
  }

  private static LocationHierarchyEvent getLocationHierarchyEvent(LocationHierarchyEvent locationHierarchyEvent) {
    return LocationHierarchyEvent.builder()
        .name(
            locationHierarchyEvent
                .getName())
        .nodeOrder(
            locationHierarchyEvent
                .getNodeOrder())
        .identifier(
            locationHierarchyEvent
                .getIdentifier())
        .build();
  }

}
