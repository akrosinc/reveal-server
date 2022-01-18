package com.revealprecision.revealserver.service;

import com.cosium.spring.data.jpa.entity.graph.domain.EntityGraphUtils;
import com.revealprecision.revealserver.api.v1.dto.factory.PlanEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.ActionRequest;
import com.revealprecision.revealserver.api.v1.dto.request.PlanRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Plan.Fields;
import com.revealprecision.revealserver.persistence.repository.FormRepository;
import com.revealprecision.revealserver.persistence.repository.PlanRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PlanService {

  private final PlanRepository planRepository;
  private final FormRepository formRepository;

  public Plan getPlanByIdentifier(UUID planIdentifier) {
    return planRepository.findById(planIdentifier).orElseThrow(() ->
        new NotFoundException(Pair.of(Fields.identifier, planIdentifier), Plan.class));
  }

  public Page<Plan> getAll(String search, Pageable pageable) {
    return planRepository.getAll(search, pageable,
        EntityGraphUtils.fromAttributePaths(
            String.join(".", Fields.goals, Goal.Fields.actions),
            String.join(".", Fields.goals, Goal.Fields.targets)));
  }

  public long getAllCount(String search) {
    return planRepository.getAllCount(search);
  }

  public void createPlan(PlanRequest planRequest) {
    if (planRequest.getActions() != null) {
      checkForms(planRequest);
    }
    Plan plan = PlanEntityFactory.toEntity(planRequest);
    plan.setEntityStatus(EntityStatus.ACTIVE);
    planRepository.save(plan);

  }

  public void checkForms(PlanRequest planRequest) {
    var forms = planRequest.getActions().stream()
        .map(ActionRequest::getDefinitionUri).collect(
            Collectors.toList());

    List<String> foundForms = formRepository.getFormsByName(forms);

    forms.removeAll(foundForms);
    if (forms.size() > 0) {
      throw new NotFoundException("Forms: " + forms + " not found");
    }
  }
}