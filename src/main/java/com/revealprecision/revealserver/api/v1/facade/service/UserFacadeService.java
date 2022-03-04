package com.revealprecision.revealserver.api.v1.facade.service;

import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.PlanService;
import com.revealprecision.revealserver.service.UserService;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserFacadeService {

  private final PlanService planService;
  private final UserService userService;
  private final LocationService locationService;

  public Set<Location> getLocationsAssignedToCurrentUser() {
    return locationService.getAssignedLocationsFromPlans(getPlansAssignedToCurrentUser());
  }

  public Set<Plan> getPlansAssignedToCurrentUser() {
    return geAssignedPlans(getCurrentUser());
  }

  public Set<Plan> geAssignedPlans(User user) {
    //TODO: final logic for finding the plans assigned to user once we know assignment (maybe obtain via organization assigned to user)00
    UUID planIdentifier = UUID.fromString("028290a5-cc4b-48cd-a041-75415fb4b0c1");
    Set<Plan> plans = new HashSet<>();
    plans.add(planService.getPlanByIdentifier(planIdentifier));
    return plans;
  }

  public Set<Organization> getOrganizationsAssignedToCurrentUser() {
    //Note: we are doing 1 organization assignment for now
    return getCurrentUser().getOrganizations();
  }

  private User getCurrentUser() {
    return userService.getCurrentUser();
  }
}
