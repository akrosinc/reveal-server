package com.revealprecision.revealserver.api.v1.facade.factory;


import com.revealprecision.revealserver.api.v1.facade.models.LocationFacade;
import com.revealprecision.revealserver.api.v1.facade.models.LocationTree;
import com.revealprecision.revealserver.api.v1.facade.models.LoginResponse;
import com.revealprecision.revealserver.api.v1.facade.models.TeamMember;
import com.revealprecision.revealserver.api.v1.facade.models.UserFacadeResponse;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.User;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class LoginResponseFactory {

  public static LoginResponse fromEntities(User user, Organization organization,
      Map<Plan, Set<Location>> assignedLocationsPerPlan) {
    UserFacadeResponse userFacadeResponse = UserFacadeResponseFactory.fromEntity(user);
    Set<Location> assignedLocations = assignedLocationsPerPlan.values().stream().flatMap(
        Collection::stream).collect(Collectors.toSet());
    Set<String> jurisdictionIds = extractJurisdictionIdentifiers(assignedLocations);
    List<String> jurisdictionNames = extractJurisdictionNames(assignedLocations);

    TeamMember teamMember = TeamMemberResponseFactory.fromEntities(organization, user);

    LocationHierarchy locationHierarchy;
    LocationTree locationTree = null;

    Set<Plan> plans = assignedLocationsPerPlan.keySet();
    if (!plans.isEmpty()) {
      //We pick one hierarchy for now:
      locationHierarchy = plans.stream().findFirst().get().getLocationHierarchy();
      List<LocationRelationship> locationRelationships = locationHierarchy.getLocationRelationships();
      List<LocationFacade> locationFacades = assignedLocations.stream().map(
              location -> LocationFacadeResponseFactory
                  .fromLocationEntityAndLocationRelationship(location, locationRelationships,
                      plans.stream()
                          .filter(plan -> assignedLocationsPerPlan.get(plan).contains(location))
                          .collect(
                              Collectors.toSet())))
          .collect(
              Collectors.toList());

      locationTree = new LocationTree();
      locationTree.buildTreeFromList(locationFacades);

      String highestGeoLevelInPlanAssignment = locationHierarchy.getNodeOrder().get(0);

      Optional<Location> defaultLocationFromPlanAssignment = assignedLocations.stream().filter(
          assignedLocation -> assignedLocation.getGeographicLevel().getName()
              .equals(highestGeoLevelInPlanAssignment)).findFirst();
      if (defaultLocationFromPlanAssignment.isPresent()) {
        log.info("default location for team {} is {}", teamMember.getTeam().getTeamName(),
            defaultLocationFromPlanAssignment.get().getName());
        teamMember.getTeam().setLocation(
            TeamLocationResponseFactory.fromEntity(defaultLocationFromPlanAssignment.get()));
      } else {
        Location defaultLocation = locationRelationships.stream()
            .filter(locationRelationship -> locationRelationship.getParentLocation() == null)
            .map(LocationRelationship::getLocation).findFirst().get();
        log.info("using default location for team from relationship {} is {}",
            teamMember.getTeam().getTeamName(), defaultLocation.getName());

        teamMember.getTeam().setLocation(TeamLocationResponseFactory.fromEntity(defaultLocation));
      }
    }

    LoginResponse loginResponse = LoginResponse.builder().user(userFacadeResponse).team(teamMember)
        .jurisdictionIds(jurisdictionIds).jurisdictions(jurisdictionNames).locations(locationTree)
        .build();
    return loginResponse;
  }

  private static List<String> extractJurisdictionNames(Set<Location> assignedLocations) {
    return assignedLocations.stream().map(Location::getName)
        .collect(
            Collectors.toList());
  }

  private static Set<String> extractJurisdictionIdentifiers(Set<Location> assignedLocations) {
    return assignedLocations.stream()
        .map(location -> location.getIdentifier().toString()).collect(
            Collectors.toSet());
  }

}
