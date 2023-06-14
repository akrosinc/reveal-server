package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.EffectivePeriod;
import com.revealprecision.revealserver.api.v1.dto.response.LocationHierarchyResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PlanResponse;
import com.revealprecision.revealserver.enums.SummaryEnum;
import com.revealprecision.revealserver.persistence.domain.Plan;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlanResponseFactory {

  public static PlanResponse fromEntity(Plan plan) {
    var goals = plan.getGoals().stream()
        .map(GoalResponseFactory::fromEntity)
        .collect(Collectors.toSet());

    return PlanResponse.builder()
        .identifier(plan.getIdentifier())
        .effectivePeriod(EffectivePeriod.builder()
            .start(plan.getEffectivePeriodStart())
            .end(plan.getEffectivePeriodEnd()).build())
        .name(plan.getName())
        .title(plan.getTitle())
        .locationHierarchy(
            LocationHierarchyResponse.builder()
                .identifier(plan.getLocationHierarchy().getIdentifier())
                .name(plan.getLocationHierarchy().getName())
                .build())
        .status(plan.getStatus())
        .interventionType(
            LookupInterventionTypeResponseFactory.fromEntity(plan.getInterventionType()))
        .goals(goals)
        .planTargetType(plan.getPlanTargetType().getGeographicLevel().getName())
        .date(plan.getDate())
        .build();
  }

  public static PlanResponse fromEntitySummary(Plan plan) {
    return PlanResponse.builder()
        .identifier(plan.getIdentifier())
        .effectivePeriod(EffectivePeriod.builder()
            .start(plan.getEffectivePeriodStart())
            .end(plan.getEffectivePeriodEnd()).build())
        .name(plan.getName())
        .date(plan.getDate())
        .title(plan.getTitle())
        .locationHierarchy(
            LocationHierarchyResponse.builder()
                .identifier(plan.getLocationHierarchy().getIdentifier())
                .name(plan.getLocationHierarchy().getName())
                .build())
        .status(plan.getStatus())
        .interventionType(
            LookupInterventionTypeResponseFactory.fromEntity(plan.getInterventionType()))
        .build();
  }

  public static Page<PlanResponse> fromEntityPage(Page<Plan> plans, Pageable pageable,
      SummaryEnum summary) {
    var response = plans.getContent().stream()
        .map(summary.equals(SummaryEnum.TRUE) ? PlanResponseFactory::fromEntitySummary
            : PlanResponseFactory::fromEntity)
        .collect(Collectors.toList());
    return new PageImpl<>(response, pageable, plans.getTotalElements());
  }
}
