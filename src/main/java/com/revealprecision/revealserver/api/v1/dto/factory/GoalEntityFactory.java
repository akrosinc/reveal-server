package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.GoalRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.persistence.domain.Form;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Plan;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GoalEntityFactory {

  public static Goal toEntity(GoalRequest request, Plan plan, Map<UUID, Form> forms) {
    Goal goal = Goal.builder()
        .identifier(request.getIdentifier())
        .description(request.getDescription())
        .priority(request.getPriority())
        .plan(plan)
        .build();

    if (request.getTargets() != null) {
      var targets = request.getTargets()
          .stream()
          .map(targetRequest -> {
            return TargetEntityFactory.toEntity(targetRequest, goal);
          })
          .collect(Collectors.toSet());
      goal.setTargets(targets);
    }

    if (request.getActions() != null) {
      var actions = request.getActions()
          .stream()
          .map(actionRequest -> {
            return ActionEntityFactory.toEntity(actionRequest, goal, forms);
          })
          .collect(Collectors.toSet());
      goal.setActions(actions);
    }

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
