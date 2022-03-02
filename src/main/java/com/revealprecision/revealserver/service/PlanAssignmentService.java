package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import com.revealprecision.revealserver.persistence.repository.PlanAssignmentRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlanAssignmentService {

  private final PlanAssignmentRepository planAssignmentRepository;

  @Autowired
  public PlanAssignmentService(PlanAssignmentRepository planAssignmentRepository){
    this.planAssignmentRepository = planAssignmentRepository;
  }


  public List<PlanAssignment> getPlanAssignmentsByOrganizationIdentifier(UUID organizationIdentifier) {
    return planAssignmentRepository.findPlanAssignmentByOrganization_Identifier(organizationIdentifier);
  }

  public List<PlanAssignment> getPlanAssignmentsByPlanIdentifier(UUID planIdentifier) {
    return planAssignmentRepository.findPlanAssignmentByPlanLocations_Plan_Identifier(planIdentifier);
  }
}