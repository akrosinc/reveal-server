package com.revealprecision.revealserver.api.v1.facade.factory;

import com.revealprecision.revealserver.api.v1.facade.models.GoalFacade;
import com.revealprecision.revealserver.enums.PriorityEnum;
import com.revealprecision.revealserver.persistence.domain.Goal;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GoalFacadeFactory {

  public static GoalFacade fromEntity(Goal goal) {
    return GoalFacade.builder()
        .id(goal.getIdentifier().toString())
        .description(goal.getDescription())
        .priority(goal.getPriority()!=null?goal.getPriority().toString(): PriorityEnum.MEDIUM_PRIORITY.name())
        .targets(null)
        .build();
  }
}