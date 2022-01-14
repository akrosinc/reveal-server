package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.ActionRequest;
import com.revealprecision.revealserver.enums.ActionSubjectEnum;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.LookupUtil;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Goal;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionEntityFactory {

  public static Action toEntity(ActionRequest actionRequest, Goal goal) {
    Action action = Action.builder()
        .code(actionRequest.getCode())
        .title(actionRequest.getTitle())
        .description(actionRequest.getDescription())
        .timingPeriodStart(actionRequest.getTimingPeriod().getStart())
        .timingPeriodEnd(actionRequest.getTimingPeriod().getEnd())
        .goal(goal)
        .subject(LookupUtil.lookup(ActionSubjectEnum.class,
            actionRequest.getSubjectCodableConcept().getText()))
        .definitionUri(actionRequest.getDefinitionUri())
        .type(actionRequest.getType())
        .reason(actionRequest.getReason())
        .build();
    action.setEntityStatus(EntityStatus.ACTIVE);
    return action;
  }
}
