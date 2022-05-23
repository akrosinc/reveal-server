package com.revealprecision.revealserver.api.v1.facade.service;

import com.revealprecision.revealserver.api.v1.facade.request.PlanRequestFacade;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.repository.PlanAssignmentRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PlanFacadeService {

  private final PlanAssignmentRepository planAssignmentRepository;


  public Set<Plan> getPlans(PlanRequestFacade planRequestFacade) {
    List<PlanAssignment> planAssignments = planAssignmentRepository.findPlansByOrganization(planRequestFacade.getOrganizations());
    return planAssignments.stream()
        .map(planAssignment -> planAssignment.getPlanLocations().getPlan()).filter(plan -> plan.getServerVersion() >= planRequestFacade.getServerVersion())
        .collect(Collectors.toSet());
  }
}