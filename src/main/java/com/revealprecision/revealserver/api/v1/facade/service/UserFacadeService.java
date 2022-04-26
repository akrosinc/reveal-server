package com.revealprecision.revealserver.api.v1.facade.service;

import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.UserService;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserFacadeService {

  private final UserService userService;
  private final LocationService locationService;

  public Set<Location> getLocationsAssignedToCurrentUser() {
    return locationService.getAssignedLocationsFromPlanAssignments(getPlanAssignmentsForCurrentUser());
  }

  public Set<Plan> getPlansAssignedToCurrentUser() {
    return geAssignedPlans();
  }

  private Set<PlanAssignment> getPlanAssignmentsForCurrentUser(){
    Set<Organization> organizations = getOrganizationsAssignedToCurrentUser();
    return organizations.stream().map(Organization::getPlanAssignments).flatMap(
        Collection::stream).collect(Collectors.toSet());
  }
  public Set<Plan> geAssignedPlans() {
    Set<Organization> organizations = getOrganizationsAssignedToCurrentUser();
    Set<Plan> assignedPlans = organizations.stream().map(Organization::getPlanAssignments).flatMap(
        Collection::stream).map(PlanAssignment::getPlanLocations).map(
        PlanLocations::getPlan).collect(Collectors.toSet());
    return assignedPlans;
  }

  public Set<Organization> getOrganizationsAssignedToCurrentUser() {
    return getCurrentUser().getOrganizations();
  }

  private User getCurrentUser() {
    return userService.getCurrentUser();
  }
}
