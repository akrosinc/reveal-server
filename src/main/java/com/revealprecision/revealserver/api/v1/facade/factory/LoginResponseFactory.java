package com.revealprecision.revealserver.api.v1.facade.factory;


import com.revealprecision.revealserver.api.v1.facade.models.LocationFacade;
import com.revealprecision.revealserver.api.v1.facade.models.LocationTree;
import com.revealprecision.revealserver.api.v1.facade.models.LoginResponse;
import com.revealprecision.revealserver.api.v1.facade.models.Team;
import com.revealprecision.revealserver.api.v1.facade.models.TeamMember;
import com.revealprecision.revealserver.api.v1.facade.models.UserFacadeResponse;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.User;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginResponseFactory {

  public static LoginResponse fromEntities(User user, Organization organization,
      Set<Location> assignedLocations, Set<Plan> plans) {
    UserFacadeResponse userFacadeResponse = UserFacadeResponseFactory.fromEntity(user);
    Team team = TeamResponseFactory.fromEntity(organization);
    TeamMember teamMember = TeamMemberResponseFactory.fromEntities(organization, user);
    teamMember.setTeam(team);
    Set<String> jurisdictionIds = extractJurisdictionIdentifiers(assignedLocations);
    List<String> jurisdictionNames = extractJurisdictionNames(assignedLocations);

    //We pick one hierarchy for now:
    LocationHierarchy locationHierarchy = plans.stream().findFirst().get().getLocationHierarchy();
    List<LocationRelationship> locationRelationships = locationHierarchy.getLocationRelationships();

    List<LocationFacade> locationFacades = assignedLocations.stream().map(
        location -> LocationFacadeResponseFactory
            .fromLocationEntityAndLocationRelationship(location, locationRelationships)).collect(
        Collectors.toList());

    LocationTree locationTree = new LocationTree();
    locationTree.buildTreeFromList(locationFacades);

    LoginResponse loginResponse = LoginResponse.builder().user(userFacadeResponse).team(teamMember)
        .jurisdictionIds(jurisdictionIds).jurisdictions(jurisdictionNames).locations(locationTree)
        .build();
    return loginResponse;
  }

  private static List<String> extractJurisdictionNames(Set<Location> assignedLocations) {
    return assignedLocations.stream().map(location -> location.getName())
        .collect(
            Collectors.toList());
  }

  private static Set<String> extractJurisdictionIdentifiers(Set<Location> assignedLocations) {
    return assignedLocations.stream()
        .map(location -> location.getIdentifier().toString()).collect(
            Collectors.toSet());
  }

}
