package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.ActionRequest;
import com.revealprecision.revealserver.api.v1.dto.request.GoalRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Plan;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GoalEntityFactory {

  public static Goal toEntity(GoalRequest request, Plan plan, Set<ActionRequest> actions) {
    Goal goal = Goal.builder()
        .identifier(request.getIdentifier())
        .description(request.getDescription())
        .priority(request.getPriority())
        .plan(plan)
        .build();

    var targets = request.getTargets()
        .stream()
        .map(targetRequest -> {
          return TargetEntityFactory.toEntity(targetRequest, goal);
        })
        .collect(Collectors.toSet());

    var addActions = actions
        .stream()
        .filter(actionRequest -> actionRequest.getGoalId().equals(request.getIdentifier()))
        .map(actionRequest -> {
          return ActionEntityFactory.toEntity(actionRequest, goal);
        })
        .collect(Collectors.toSet());

    goal.setTargets(targets);
    goal.setActions(addActions);
    goal.setEntityStatus(EntityStatus.ACTIVE);
    return goal;
  }

  public static Goal toEntityWithoutAction(GoalRequest goalRequest, Plan plan) {

    Goal goal = Goal.builder()
        .identifier(goalRequest.getIdentifier())
        .description(goalRequest.getDescription())
        .priority(goalRequest.getPriority())
        .plan(plan)
        .build();

    var targets = goalRequest.getTargets()
        .stream()
        .map(targetRequest -> TargetEntityFactory.toEntity(targetRequest, goal))
        .collect(Collectors.toSet());

    goal.setTargets(targets);
    goal.setEntityStatus(EntityStatus.ACTIVE);
    return goal;
  }
}
