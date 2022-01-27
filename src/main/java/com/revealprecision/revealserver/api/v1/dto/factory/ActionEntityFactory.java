package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.ActionRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Form;
import com.revealprecision.revealserver.persistence.domain.Goal;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionEntityFactory {

  public static Action toEntity(ActionRequest actionRequest, Goal goal, Map<UUID, Form> forms) {
    Action action = Action.builder()
        .title(actionRequest.getTitle())
        .description(actionRequest.getDescription())
        .timingPeriodStart(actionRequest.getTimingPeriod().getStart())
        .timingPeriodEnd(actionRequest.getTimingPeriod().getEnd())
        .goal(goal)
        .form(forms.get(actionRequest.getFormIdentifier()))
        .type(actionRequest.getType())
        .reason(actionRequest.getReason())
        .build();
    action.setEntityStatus(EntityStatus.ACTIVE);
    return action;
  }

  public static Action toEntity(ActionRequest actionRequest, Goal goal, Form form) {
    Action action = Action.builder()
        .title(actionRequest.getTitle())
        .description(actionRequest.getDescription())
        .timingPeriodStart(actionRequest.getTimingPeriod().getStart())
        .timingPeriodEnd(actionRequest.getTimingPeriod().getEnd())
        .goal(goal)
        .form(form)
        .type(actionRequest.getType())
        .reason(actionRequest.getReason())
        .build();
    action.setEntityStatus(EntityStatus.ACTIVE);
    return action;
  }
}