package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.ConditionEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.ConditionRequest;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Condition;
import com.revealprecision.revealserver.persistence.domain.Condition.Fields;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.repository.ConditionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConditionService {

  private final ConditionRepository conditionRepository;
  private final PlanService planService;
  private final GoalService goalService;
  private final ActionService actionService;

  public Page<Condition> getConditions(UUID planIdentifier, UUID goalIdentifier,
      UUID actionIdentifier, Pageable pageable) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    Goal goal = goalService.findByIdentifier(goalIdentifier);
    Action action = actionService.getByIdentifier(actionIdentifier);

    return conditionRepository.getAllByPlanId(actionIdentifier, pageable);
  }

  public Condition getCondition(UUID identifier) {
    return conditionRepository.findById(identifier).orElseThrow(() -> new NotFoundException(Pair.of(
        Fields.identifier, identifier), Condition.class));
  }

  public void createCondition(UUID planIdentifier, UUID goalIdentifier,
      UUID actionIdentifier, ConditionRequest request) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    Goal goal = goalService.findByIdentifier(goalIdentifier);
    Action action = actionService.getByIdentifier(actionIdentifier);

    Condition condition = ConditionEntityFactory.toEntity(request, action);
    conditionRepository.save(condition);
  }

  public void deleteCondition(UUID planIdentifier, UUID goalIdentifier,
      UUID actionIdentifier, UUID conditionIdentifier) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    Goal goal = goalService.findByIdentifier(goalIdentifier);
    Action action = actionService.getByIdentifier(actionIdentifier);
    Condition condition = getCondition(conditionIdentifier);

    conditionRepository.delete(condition);
  }
}
