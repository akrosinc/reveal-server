package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.ConditionRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Condition;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConditionEntityFactory {

  public static Condition toEntity(ConditionRequest conditionRequest, Action action) {
    Condition condition = Condition.builder()
        .name("test")
        .query("test")
        .implicit_query("test")
        .action(action)
        .build();

    if (conditionRequest.getTargets() != null) {
      var targets = conditionRequest.getTargets()
          .stream()
          .map(targetRequest -> {
            return TargetEntityFactory.toEntity(targetRequest, condition);
          })
          .collect(Collectors.toSet());
      condition.setTargets(targets);
    }
    condition.setEntityStatus(EntityStatus.ACTIVE);
    return condition;
  }
}
