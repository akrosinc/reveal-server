package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.TargetResponse;
import com.revealprecision.revealserver.persistence.domain.Target;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TargetResponseFactory {

  public static TargetResponse fromEntity(Target target) {
    return TargetResponse.builder()
        .identifier(target.getIdentifier())
        .comparator(target.getComparator())
        .measure(target.getMeasure())
        .due(target.getDue())
        .value(target.getValue())
        .unit(target.getUnit())
        .build();
  }
}
