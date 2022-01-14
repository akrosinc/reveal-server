package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.PlanEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.PlanRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Plan.Fields;
import com.revealprecision.revealserver.persistence.repository.FormRepository;
import com.revealprecision.revealserver.persistence.repository.PlanRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
public class PlanService {

  private static final Logger logger = LoggerFactory.getLogger(PlanService.class);
  private final PlanRepository planRepository;
  private final ProducerService producerService;
  private final FormRepository formRepository;

  @Autowired
  public PlanService(PlanRepository planRepository, ProducerService producerService,
      FormRepository formRepository) {
    this.planRepository = planRepository;
    this.producerService = producerService;
    this.formRepository = formRepository;
  }

  public Plan getPlanByIdentifier(UUID planIdentifier) {
    return planRepository.findById(planIdentifier).orElseThrow(() ->
        new NotFoundException(Pair.of(Fields.identifier, planIdentifier), Plan.class));
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
        .map(actionRequest -> actionRequest.getDefinitionUri()).collect(
            Collectors.toList());

    List<String> foundForms = formRepository.getFormsByName(forms);

    forms.removeAll(foundForms);
    if (forms.size() > 0) {
      throw new NotFoundException("Forms: " + forms.toString() + " not found");
    }
  }
}