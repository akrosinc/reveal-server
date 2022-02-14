package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.TargetRequest;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Condition;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Target;
import com.revealprecision.revealserver.persistence.domain.Target.Fields;
import com.revealprecision.revealserver.persistence.repository.TargetRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class TargetService {

  private final TargetRepository targetRepository;
  private final PlanService planService;
  private final GoalService goalService;
  private final ActionService actionService;
  private final ConditionService conditionService;

  public Target getByIdentifier(UUID identifier) {
    return targetRepository.findById(identifier).orElseThrow(() -> new NotFoundException(Pair.of(
        Fields.identifier, identifier), Target.class));
  }

  public void createTarget(TargetRequest targetRequest, UUID planIdentifier,
      UUID goalIdentifier) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    Goal goal = goalService.findByIdentifier(goalIdentifier);
    //TODO: finish target creation
  }

  public void updateTarget(TargetRequest targetRequest, UUID planIdentifier, UUID goalIdentifier,
      UUID targetIdentifier) {
    Target target = getByIdentifier(targetIdentifier);
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    Goal goal = goalService.findByIdentifier(goalIdentifier);

    target.update(targetRequest);
    targetRepository.save(target);
  }

  public Page<Target> getAll(UUID planIdentifier, UUID goalIdentifier, UUID actionIdentifier,
      UUID conditionIdentifier, Pageable pageable) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    Goal goal = goalService.findByIdentifier(goalIdentifier);
    Action action = actionService.getByIdentifier(actionIdentifier);
    Condition condition = conditionService.getCondition(conditionIdentifier);

    return targetRepository.getAll(conditionIdentifier, pageable);
  }

}
