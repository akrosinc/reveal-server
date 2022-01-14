package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.ActionEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.ActionRequest;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Action.Fields;
import com.revealprecision.revealserver.persistence.domain.Form;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.repository.ActionRepository;
import com.revealprecision.revealserver.persistence.repository.FormRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ActionService {

  private final ActionRepository actionRepository;
  private final GoalService goalService;
  private final PlanService planService;
  private final FormRepository formRepository;

  public Action getByIdentifier(UUID identifier) {
    return actionRepository.findById(identifier).orElseThrow(() -> new NotFoundException(Pair.of(
        Fields.identifier, identifier), Action.class));
  }

  public void createAction(UUID planIdentifier, String goalIdentifier,
      ActionRequest actionRequest) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    Goal goal = goalService.findByIdentifier(goalIdentifier);
    validateData(plan, goal, actionRequest.getDefinitionUri());

    Action action = ActionEntityFactory.toEntity(actionRequest, goal);
    actionRepository.save(action);
  }

  public void updateAction(UUID planIdentifier, String goalIdentifier,
      ActionRequest actionRequest, UUID actionIdentifier) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    Goal goal = goalService.findByIdentifier(goalIdentifier);
    validateData(plan, goal, actionRequest.getDefinitionUri());

    Action action = getByIdentifier(actionIdentifier);
    action.update(actionRequest);
    actionRepository.save(action);
  }

  public Page<Action> getActions(UUID planIdentifier, String goalIdentifier, Pageable pageable) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    Goal goal = goalService.findByIdentifier(goalIdentifier);
    if (!plan.getGoals().contains(goal)) {
      throw new ConflictException(Goal.class, goal.getIdentifier(), Plan.class,
          plan.getIdentifier());
    }
    return actionRepository.getActions(goalIdentifier, pageable);
  }

  public void validateData(Plan plan, Goal goal, String formName) {
    formRepository.findByName(formName)
        .orElseThrow(() -> new NotFoundException(Pair.of(
            Form.Fields.name, formName), Form.class));
    if (!plan.getGoals().contains(goal)) {
      throw new ConflictException(Goal.class, goal.getIdentifier(), Plan.class,
          plan.getIdentifier());
    }
  }
}
