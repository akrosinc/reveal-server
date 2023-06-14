package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.ConditionEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.ConditionRequest;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Condition;
import com.revealprecision.revealserver.persistence.domain.Condition.Fields;
import com.revealprecision.revealserver.persistence.repository.ConditionRepository;
import java.util.List;
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
  private final ActionService actionService;

  public Page<Condition> getConditions(
      UUID actionIdentifier, Pageable pageable) {
    return conditionRepository.getAllByPlanId(actionIdentifier, pageable);
  }

  public Condition getCondition(UUID identifier) {
    return conditionRepository.findById(identifier).orElseThrow(() -> new NotFoundException(Pair.of(
        Fields.identifier, identifier), Condition.class));
  }

  public void createCondition(
      UUID actionIdentifier, ConditionRequest request) {
    Action action = actionService.getByIdentifier(actionIdentifier);

    Condition condition = ConditionEntityFactory.toEntity(request, action);
    conditionRepository.save(condition);
  }

  public void deleteCondition(UUID conditionIdentifier) {
    Condition condition = getCondition(conditionIdentifier);

    conditionRepository.delete(condition);
  }
  public List<Condition> getConditionsByActionIdentifier(UUID actionIdentifier){
   return conditionRepository.findConditionByAction_Identifier(actionIdentifier);
  }
}
