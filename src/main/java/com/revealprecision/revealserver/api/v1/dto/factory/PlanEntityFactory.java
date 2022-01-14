package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.PlanRequest;
import com.revealprecision.revealserver.enums.LookupUtil;
import com.revealprecision.revealserver.enums.PlanInterventionTypeEnum;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import com.revealprecision.revealserver.persistence.domain.Plan;
import java.util.Date;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlanEntityFactory {

  public static Plan toEntity(PlanRequest planRequest) {
    var wrapper = new Object() {
      PlanInterventionTypeEnum type;
    };
    planRequest.getUseContext().forEach(context -> {
      switch (context.getCode()) {
        case "interventionType":
          wrapper.type = LookupUtil.lookup(PlanInterventionTypeEnum.class,
              context.getValueCodableConcept());
      }
    });

    Plan plan = Plan.builder()
        .name(planRequest.getName())
        .title(planRequest.getTitle())
        .date(new Date())
        .effectivePeriodStart(planRequest.getEffectivePeriod().getStart())
        .effectivePeriodEnd(planRequest.getEffectivePeriod().getEnd())
        .status(PlanStatusEnum.DRAFT)
        .interventionType(wrapper.type)
        .build();

    if (planRequest.getGoals() != null) {
      var goals = planRequest.getGoals()
          .stream()
          .map(request -> {
            return GoalEntityFactory.toEntity(request, plan, planRequest.getActions());
          })
          .collect(Collectors.toSet());
      plan.setGoals(goals);
    }
    return plan;
  }
}
