package com.revealprecision.revealserver.api.v1.facade.service;

import static java.util.stream.Collectors.toSet;

import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.UserService;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserFacadeService {

  private final UserService userService;
  private final LocationService locationService;

  public Map<Plan, Set<Location>> getLocationsAssignedToCurrentUser() {
    return locationService.getAssignedLocationsFromPlanAssignments(
        getPlanAssignmentsForCurrentUser());
  }


  private Set<PlanAssignment> getPlanAssignmentsForCurrentUser() {
    Set<Organization> organizations = getOrganizationsAssignedToCurrentUser();
    return organizations.stream().map(Organization::getPlanAssignments).flatMap(
        Collection::stream).collect(toSet());
  }

  public Set<Organization> getOrganizationsAssignedToCurrentUser() {
    return getCurrentUser().getOrganizations();
  }

  private User getCurrentUser() {
    return userService.getCurrentUser();
  }
}
