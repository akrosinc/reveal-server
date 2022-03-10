package com.revealprecision.revealserver.api.v1.facade.factory;

import com.revealprecision.revealserver.api.v1.dto.request.EffectivePeriod;
import com.revealprecision.revealserver.api.v1.facade.models.ActionFacade;
import com.revealprecision.revealserver.api.v1.facade.models.JurisdictionFacade;
import com.revealprecision.revealserver.api.v1.facade.models.PlanFacade;
import com.revealprecision.revealserver.api.v1.facade.models.UseContext;
import com.revealprecision.revealserver.persistence.domain.Plan;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlanFacadeFactory {

  public static PlanFacade fromEntity(Plan plan) {
    List<UseContext> context = new ArrayList<>();
    context.add(new UseContext("interventionType", plan.getInterventionType().getCode()));
    context.add(new UseContext("locationHierarchy",
        plan.getLocationHierarchy().getIdentifier().toString()));

    var goals = plan.getGoals().stream().map(GoalFacadeFactory::fromEntity)
        .collect(Collectors.toList());

    List<ActionFacade> actions = new ArrayList<>();
    plan.getGoals().forEach(goal -> {
      var addActions = goal.getActions().stream().map(ActionFacadeFactory::fromEntity).collect(
          Collectors.toList());
      actions.addAll(addActions);
    });

    List<JurisdictionFacade> jurisdictions = plan.getPlanLocations().stream()
        .filter(planLocations -> planLocations.getLocation().getGeographicLevel().getName().equals("operational"))
        .map(planLocations -> new JurisdictionFacade(planLocations.getLocation().getIdentifier().toString()))
        .collect(Collectors.toList());

    return PlanFacade.builder()
        .identifier(plan.getIdentifier().toString())
        .name(plan.getName())
        .title(plan.getTitle())
        .status(plan.getStatus())
        .date(plan.getDate())
        .effectivePeriod(EffectivePeriod.builder()
            .start(plan.getEffectivePeriodStart())
            .end(plan.getEffectivePeriodEnd()).build())
        .useContext(context)
        .jurisdiction(jurisdictions)
        .goals(goals)
        .actions(actions)
        .build();
  }
}