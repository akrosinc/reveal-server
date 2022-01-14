package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.GoalEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.GoalRequest;
import com.revealprecision.revealserver.api.v1.dto.request.GoalUpdateRequest;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.constant.Error;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Goal.Fields;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.repository.GoalRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class GoalService {

  private final GoalRepository goalRepository;
  private final PlanService planService;

  public Goal findByIdentifier(String identifier) {
    return goalRepository.findById(identifier).orElseThrow(() -> new NotFoundException(Pair.of(
        Fields.identifier, identifier), Goal.class));
  }

  public void createGoal(UUID identifier, GoalRequest goalRequest) {
    if (goalRepository.findById(goalRequest.getIdentifier()).isPresent()) {
      throw new ConflictException(
          String.format(Error.NON_UNIQUE, Fields.identifier, goalRequest.getIdentifier()));
    }
    Plan plan = planService.getPlanByIdentifier(identifier);
    Goal goal = GoalEntityFactory.toEntityWithoutAction(goalRequest, plan);
    goalRepository.save(goal);
  }

  public void updateGoal(String identifier, UUID planIdentifier,
      GoalUpdateRequest goalUpdateRequest) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    Goal goal = findByIdentifier(identifier);
    if (!plan.getGoals().contains(goal)) {
      throw new ConflictException(Goal.class, identifier, Plan.class, planIdentifier);
    }
    goal.update(goalUpdateRequest);
    goalRepository.save(goal);
  }

  public Page<Goal> getGoals(UUID identifier, Pageable pageable) {
    return goalRepository.getAllFromPlan(identifier, pageable);
  }
}
