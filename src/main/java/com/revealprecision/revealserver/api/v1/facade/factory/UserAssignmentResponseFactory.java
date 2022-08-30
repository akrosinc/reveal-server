package com.revealprecision.revealserver.api.v1.facade.factory;

import static java.util.stream.Collectors.toSet;

import com.revealprecision.revealserver.api.v1.facade.models.UserAssignmentResponse;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Plan;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserAssignmentResponseFactory {

  public static UserAssignmentResponse fromEntities(Set<Organization> assignedOrganizations,
      Map<Plan, Set<Location>> assignedLocationsPerPlan) {
    Set<Plan> assignedPlans = assignedLocationsPerPlan.keySet();
    Set<Location> assignedLocations = assignedLocationsPerPlan.values().stream().flatMap(
        Collection::stream).collect(toSet());
    UserAssignmentResponse userAssignmentResponse = UserAssignmentResponse.builder().plans(
            extractPlanIdentifiers(assignedPlans))
        .organizationIds(extractOrganizationIdentifiers(assignedOrganizations))
        .jurisdictions(extractJurisdictionIdentifiers(assignedLocations)).build();
    return userAssignmentResponse;
  }

  private static Set<String> extractPlanIdentifiers(Set<Plan> assignedPlans) {
    return assignedPlans.stream().map(Plan::getIdentifier).map(
        UUID::toString).collect(toSet());
  }

  private static Set<UUID> extractOrganizationIdentifiers(Set<Organization> assignedOrganizations) {
    return assignedOrganizations.stream().map(Organization::getIdentifier)
        .collect(toSet());
  }

  private static Set<String> extractJurisdictionIdentifiers(Set<Location> assignedLocations) {
    return assignedLocations.stream().map(Location::getIdentifier).map(UUID::toString)
        .collect(toSet());
  }
}
