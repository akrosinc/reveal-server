package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.TargetResponse;
import com.revealprecision.revealserver.persistence.domain.Target;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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

  public static Page<TargetResponse> fromEntityPage(Page<Target> targets, Pageable pageable) {
    var response = targets.getContent().stream()
        .map(TargetResponseFactory::fromEntity)
        .collect(Collectors.toList());
    return new PageImpl<>(response, pageable, targets.getTotalElements());
  }
}
