package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.AssignTeamHierarchyRequest;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import com.revealprecision.revealserver.persistence.repository.PlanAssignmentRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanAssignmentService {

  private final PlanAssignmentRepository planAssignmentRepository;
  private final PlanLocationsService planLocationsService;
  private final OrganizationService organizationService;

  public List<PlanAssignment> getPlanAssignmentsByOrganizationIdentifier(
      UUID organizationIdentifier) {
    return planAssignmentRepository.findPlanAssignmentsByOrganization_Identifier(
        organizationIdentifier);
  }

  public List<PlanAssignment> getPlanAssignmentsByPlanIdentifier(UUID planIdentifier) {
    return planAssignmentRepository.findPlanAssignmentsByPlanLocations_Plan_Identifier(
        planIdentifier);
  }

  public List<PlanAssignment> getPlanAssignmentByPlanLocationIdentifier(UUID planIdentifier,
      UUID locationIdentifier) {
    return planAssignmentRepository.findPlanAssignmentsByPlanLocations_Plan_IdentifierAndPlanLocations_Location_Identifier(
        planIdentifier, locationIdentifier);
  }

  @Transactional
  public void assignOrganizationsToLocation(Set<UUID> organizationIdentifiers,
      UUID locationId, UUID planId) {
    PlanLocations planLocation = planLocationsService.getPlanLocationByPlanIdentifierAndLocationIdentifier(
        planId, locationId);
    planAssignmentRepository.deletePlanAssignmentsByPlanLocations_Plan_IdentifierAndPlanLocations_Location_Identifier(
        planId, locationId);
    organizationIdentifiers.forEach(org -> {
      Organization organization = organizationService.findById(org, true);
      planAssignmentRepository.save(new PlanAssignment(organization, planLocation));
    });
  }

  @Transactional
  public void assignTeamsToLocationHierarchy(UUID planId,
      AssignTeamHierarchyRequest assignTeamHierarchyRequest) {
    if (assignTeamHierarchyRequest.getHierarchy().isEmpty()) {
      planAssignmentRepository.deletePlanAssignmentsByPlanLocations_Plan_Identifier(planId);
    } else {
      assignTeamHierarchyRequest.getHierarchy().forEach(assignTeams -> {
        if (assignTeams.getTeams().isEmpty()) {
          planAssignmentRepository.deletePlanAssignmentsByPlanLocations_Plan_IdentifierAndPlanLocations_Location_Identifier(
              planId, assignTeams.getLocationId());
        } else {
          assignOrganizationsToLocation(assignTeams.getTeams(), assignTeams.getLocationId(),
              planId);
        }
      });
    }
  }
}