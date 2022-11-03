package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.ActionRequest;
import com.revealprecision.revealserver.enums.ActionTitleEnum;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Form;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.MapUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionEntityFactory {

  public static Action toEntity(ActionRequest actionRequest, Goal goal, Map<UUID, Form> forms,
      List<LookupEntityType> lookupEntityTypes) {

    Action action = Action.builder()
        .title(actionRequest.getTitle())
        .description(actionRequest.getDescription())
        .timingPeriodStart(actionRequest.getTimingPeriod().getStart())
        .timingPeriodEnd(actionRequest.getTimingPeriod().getEnd())
        .goal(goal)
        .type(actionRequest.getType())
        .build();
    if (MapUtils.isNotEmpty(forms)) {
      action.setForm(forms.get(actionRequest.getFormIdentifier()));
    }
    action.setEntityStatus(EntityStatus.ACTIVE);
    lookupEntityTypes.stream()
        .filter(lookupEntityType -> lookupEntityType.getCode()
            .equals(ActionTitleEnum.lookup(actionRequest.getTitle())
                .getEntityType().getLookupEntityType())).findFirst()
        .ifPresent(action::setLookupEntityType);

    if (actionRequest.getConditions() != null) {
      var conditions = actionRequest.getConditions()
          .stream()
          .map(conditionRequest -> ConditionEntityFactory.toEntity(conditionRequest, action))
          .collect(Collectors.toSet());
      action.setConditions(conditions);
    }

    return action;
  }

  public static Action toEntity(ActionRequest actionRequest, Goal goal, Form form,
      LookupEntityType lookupEntityType) {
    Action action = Action.builder()
        .title(actionRequest.getTitle())
        .description(actionRequest.getDescription())
        .timingPeriodStart(actionRequest.getTimingPeriod().getStart())
        .timingPeriodEnd(actionRequest.getTimingPeriod().getEnd())
        .goal(goal)
        .form(form)
        .type(actionRequest.getType())
        .lookupEntityType(lookupEntityType)
        .build();
    action.setEntityStatus(EntityStatus.ACTIVE);
    return action;
  }
}
