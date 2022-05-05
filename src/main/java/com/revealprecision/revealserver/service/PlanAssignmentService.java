package com.revealprecision.revealserver.service;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.revealprecision.revealserver.api.v1.dto.request.AssignTeamHierarchyRequest;
import com.revealprecision.revealserver.api.v1.dto.request.MultipleLocationTeamAssignRequest;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import com.revealprecision.revealserver.persistence.repository.PlanAssignmentRepository;
import com.revealprecision.revealserver.persistence.repository.PlanLocationsRepository;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanAssignmentService {

  private final PlanAssignmentRepository planAssignmentRepository;
  private final PlanLocationsService planLocationsService;
  private final PlanLocationsRepository planLocationsRepository;
  private final OrganizationService organizationService;
  private final PlanService planService;

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


  public void assignOrganizationsToLocation(Set<UUID> organizationIdentifiers,
      UUID locationId, UUID planId) {
    PlanLocations planLocation = planLocationsService.getPlanLocationByPlanIdentifierAndLocationIdentifier(
        planId, locationId);
    System.out.println("PRE DELETE ALL: " + new Date());
    planAssignmentRepository.deletePlanAssignmentsByPlanLocations_Plan_IdentifierAndPlanLocations_Location_Identifier(
        planId, locationId);
    System.out.println("POSLE DELETE ALL: " + new Date());
    List<PlanAssignment> planAssignmentsToSave = new ArrayList<>();
    System.out.println("PRE CREATE SAVE ALL: " + new Date());
    organizationIdentifiers.forEach(org -> {
      Organization organization = organizationService.findById(org, true);
      planAssignmentsToSave.add(new PlanAssignment(organization, planLocation));
    });
    System.out.println("POSLE CREATE SAVE ALL: " + new Date());
    planAssignmentRepository.saveAll(planAssignmentsToSave);
    System.out.println("POSLE SAVE ALL: " + new Date());
  }

  @Transactional
  public void assignTeamsToLocationHierarchy(UUID planId,
      AssignTeamHierarchyRequest assignTeamHierarchyRequest) {
     // planAssignmentRepository.deletePlanAssignmentsByPlanLocations_Plan_Identifier(planId);
      Set<UUID> organizations = new HashSet<>();
      Set<UUID> locations = new HashSet<>();
      assignTeamHierarchyRequest.getHierarchy().stream().forEach(assignTeams -> {
        organizations.addAll(assignTeams.getTeams());
        locations.add(assignTeams.getLocationId());
      });
      if(organizations.isEmpty()) {
        return;
      }
      Set<Organization> orgs = organizationService.findByIdentifiers(organizations);
      Map<UUID, Organization> orgsMap = orgs.stream().collect(
          Collectors.toMap(Organization::getIdentifier, o -> o));

//      List<PlanAssignment> assignTeams = new ArrayList<>();
      Set<PlanLocations> planLocations = planLocationsService.getPlanLocationsByPlanIdAndLocationIds(planId, new ArrayList<>(locations));
      Map<UUID, PlanLocations> planLocationsMap = planLocations.stream().collect(Collectors.toMap(planLocations1 -> planLocations1.getLocation().getIdentifier(), pl -> pl));
      List<PlanAssignment> planAssignmentsToSave = new ArrayList<>();

      assignTeamHierarchyRequest.getHierarchy().stream().forEach(at -> {
        PlanLocations planLoc = planLocationsMap.get(at.getLocationId());
        List<Organization> orgsToAdd = new ArrayList<>(
            Maps.filterKeys(orgsMap, Predicates.in(at.getTeams())).values());
        orgsToAdd.forEach(organization -> planAssignmentsToSave.add(new PlanAssignment(organization, planLoc)));
      });
    System.out.println("PRE SAVE");
      planAssignmentRepository.saveAllAndFlush(planAssignmentsToSave);
    System.out.println("POSLE SAVE");
//      assignTeamHierarchyRequest.getHierarchy().forEach(assignTeams -> {
//        if (assignTeams.getTeams().isEmpty()) {
//          planAssignmentRepository.deletePlanAssignmentsByPlanLocations_Plan_IdentifierAndPlanLocations_Location_Identifier(
//              planId, assignTeams.getLocationId());
//        } else {
//          assignOrganizationsToLocation(assignTeams.getTeams(), assignTeams.getLocationId(),
//              planId);
//        }
//      });
//    }
  }

  public void assignMultipleTeams(UUID planIdentifier, MultipleLocationTeamAssignRequest request) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    List<PlanLocations> planLocations = planLocationsService.getByPlanIdAndLocationIdentifiers(plan.getIdentifier(), List.copyOf(request.getLocations()));
    if(request.getTeams().isEmpty()){
      planLocations.forEach(pl -> pl.getPlanAssignments().clear());
      planLocationsRepository.saveAll(planLocations);
    }else {
      Set<Organization> teams = organizationService.findByIdentifiers(request.getTeams());
      planLocations.forEach(pl -> {
        pl.getPlanAssignments().clear();
        teams.forEach(organization -> {
          pl.getPlanAssignments().add(new PlanAssignment(organization, pl));
        });
      });
      planLocationsRepository.saveAll(planLocations);
    }
  }
}