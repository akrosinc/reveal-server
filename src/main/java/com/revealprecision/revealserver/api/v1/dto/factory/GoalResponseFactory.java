package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.GoalResponse;
import com.revealprecision.revealserver.persistence.domain.Goal;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GoalResponseFactory {

  public static GoalResponse fromEntity(Goal goal) {
    var targets = goal.getTargets()
        .stream()
        .map(TargetResponseFactory::fromEntity)
        .collect(Collectors.toSet());

    return GoalResponse.builder()
        .identifier(goal.getIdentifier())
        .priority(goal.getPriority())
        .description(goal.getDescription())
        .targets(targets)
        .build();
  }

  public static Page<GoalResponse> fromEntityPage(Page<Goal> goals, Pageable pageable) {
    var response = goals.getContent().stream()
        .map(GoalResponseFactory::fromEntity)
        .collect(Collectors.toList());
    return new PageImpl<>(response, pageable, goals.getTotalElements());
  }
}
