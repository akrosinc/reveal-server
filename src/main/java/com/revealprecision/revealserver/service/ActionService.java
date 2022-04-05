package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.ActionEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.ActionRequest;
import com.revealprecision.revealserver.enums.ActionTitleEnum;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Action.Fields;
import com.revealprecision.revealserver.persistence.domain.Form;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.repository.ActionRepository;
import java.util.List;
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
  private final FormService formService;
  private final LookupEntityTypeService lookupEntityTypeService;

  public Action getByIdentifier(UUID identifier) {
    return actionRepository.findById(identifier).orElseThrow(() -> new NotFoundException(Pair.of(
        Fields.identifier, identifier), Action.class));
  }

  public void createAction(UUID planIdentifier, UUID goalIdentifier,
      ActionRequest actionRequest) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    Goal goal = goalService.findByIdentifier(goalIdentifier);
    Form form = formService.findById(actionRequest.getFormIdentifier());

    LookupEntityType lookupEntityTypeByCode = lookupEntityTypeService.getLookupEntityTypeByCode(
        ActionTitleEnum.lookup(actionRequest.getTitle()).getEntityType().getLookupEntityType());

    Action action = ActionEntityFactory.toEntity(actionRequest, goal, form, lookupEntityTypeByCode);

    actionRepository.save(action);
  }

  public void updateAction(UUID planIdentifier, UUID goalIdentifier,
      ActionRequest actionRequest, UUID actionIdentifier) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    Goal goal = goalService.findByIdentifier(goalIdentifier);
    Form form = formService.findById(actionRequest.getFormIdentifier());

    Action action = getByIdentifier(actionIdentifier);
    action.update(actionRequest, form);
    actionRepository.save(action);
  }

  public void deleteAction(UUID planIdentifier, UUID goalIdentifier, UUID actionIdentifier) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    Goal goal = goalService.findByIdentifier(goalIdentifier);
    Action action = getByIdentifier(actionIdentifier);

    actionRepository.delete(action);
  }

  public Page<Action> getActions(UUID planIdentifier, UUID goalIdentifier, Pageable pageable) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    Goal goal = goalService.findByIdentifier(goalIdentifier);
    if (!plan.getGoals().contains(goal)) {
      throw new ConflictException(Goal.class, goal.getIdentifier(), Plan.class,
          plan.getIdentifier());
    }
    return actionRepository.getActions(goalIdentifier, pageable);
  }

  public List<Action> getActionsByGoalIdentifier(UUID goalIdentifier) {
    return actionRepository.findActionsByGoal_Identifier(goalIdentifier);
  }

  public Action findByTitle(String title) {
    return actionRepository.findByTitle(title);
  }
}
