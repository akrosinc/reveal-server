package com.revealprecision.revealserver.api.v1.facade.service;

import com.revealprecision.revealserver.api.v1.facade.request.PlanRequestFacade;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.repository.PlanAssignmentRepository;
import com.revealprecision.revealserver.persistence.repository.PlanRepository;
import com.revealprecision.revealserver.service.PlanAssignmentService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PlanFacadeService {

  private final PlanRepository planRepository;
  private final PlanAssignmentRepository planAssignmentRepository;


  public List<Plan> getPlans(PlanRequestFacade planRequestFacade) {
    List<PlanAssignment> planAssignments = planAssignmentRepository.findPlansByOrganization(planRequestFacade.getOrganizations());
    return planAssignments.stream()
        .map(planAssignment -> planAssignment.getPlanLocations().getPlan())
        .collect(Collectors.toList());
  }
}