package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.TargetRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.persistence.domain.Condition;
import com.revealprecision.revealserver.persistence.domain.Target;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TargetEntityFactory {

  public static Target toEntity(TargetRequest targetRequest, Condition condition) {
    Target target = Target.builder()
        .comparator(targetRequest.getDetail().getDetailQuantity().getComparator())
        .value(targetRequest.getDetail().getDetailQuantity().getValue())
        .unit(targetRequest.getDetail().getDetailQuantity().getUnit())
        .due(targetRequest.getDue())
        .measure(targetRequest.getMeasure())
        .condition(condition)
        .build();
    target.setEntityStatus(EntityStatus.ACTIVE);
    return target;
  }
}
