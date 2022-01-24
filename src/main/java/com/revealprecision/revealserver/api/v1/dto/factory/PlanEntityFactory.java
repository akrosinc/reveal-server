package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.PlanRequest;
import com.revealprecision.revealserver.enums.PlanInterventionTypeEnum;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.Plan;
import java.time.LocalDate;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlanEntityFactory {

  public static Plan toEntity(PlanRequest planRequest, PlanInterventionTypeEnum type,
      LocationHierarchy locationHierarchy) {

    Plan plan = Plan.builder()
        .name(planRequest.getName())
        .title(planRequest.getTitle())
        .date(LocalDate.now())
        .effectivePeriodStart(planRequest.getEffectivePeriod().getStart())
        .effectivePeriodEnd(planRequest.getEffectivePeriod().getEnd())
        .status(PlanStatusEnum.DRAFT)
        .interventionType(type)
        .locationHierarchy(locationHierarchy)
        .build();

    if (planRequest.getGoals() != null) {
      var goals = planRequest.getGoals()
          .stream()
          .map(request -> GoalEntityFactory.toEntity(request, plan, planRequest.getActions()))
          .collect(Collectors.toSet());
      plan.setGoals(goals);
    }
    return plan;
  }
}
