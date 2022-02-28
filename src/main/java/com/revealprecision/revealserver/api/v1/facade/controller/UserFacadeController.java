package com.revealprecision.revealserver.api.v1.facade.controller;

import com.revealprecision.revealserver.api.v1.facade.dto.factory.LocationFacadeResponseFactory;
import com.revealprecision.revealserver.api.v1.facade.dto.factory.TeamMemberResponseFactory;
import com.revealprecision.revealserver.api.v1.facade.dto.factory.TeamResponseFactory;
import com.revealprecision.revealserver.api.v1.facade.dto.factory.UserFacadeResponseFactory;
import com.revealprecision.revealserver.api.v1.facade.dto.response.LocationFacade;
import com.revealprecision.revealserver.api.v1.facade.dto.response.LocationTree;
import com.revealprecision.revealserver.api.v1.facade.dto.response.LoginResponseData;
import com.revealprecision.revealserver.api.v1.facade.dto.response.UserAssignmentResponse;
import com.revealprecision.revealserver.api.v1.facade.dto.service.UserFacadeService;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.service.UserService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserFacadeController {

  private final UserService userService;
  private final UserFacadeService userFacadeService;

  @GetMapping("/security/authenticate")
  public ResponseEntity<LoginResponseData> authenticate() {
    User user = userService.getCurrentUser();
    Optional<Organization> organizationOptional = user.getOrganizations().stream().findFirst();
    ; //Assumption that practioner always assigned to one org.
    if (organizationOptional.isEmpty()) {
      //handle case when user is not assgined
    }
    Organization organization = organizationOptional.get();
    var userFacadeResponse = UserFacadeResponseFactory.fromEntity(user);
    var team = TeamResponseFactory.fromEntity(organization);
    var teamMember = TeamMemberResponseFactory.fromEntities(organization, user);
    teamMember.setTeam(team);

    List<Plan> plansAssignedToUser = userFacadeService.geAssignedPlans(user);
    Set<Location> assignedLocations = new HashSet<>();

    plansAssignedToUser.forEach(plan -> assignedLocations.addAll(plan.getLocations()));

    Set<String> jurisdictionIds = assignedLocations.stream()
        .map(location -> location.getIdentifier().toString()).collect(
            Collectors.toSet());
    List<String> locationNames = assignedLocations.stream().map(location -> location.getName())
        .collect(
            Collectors.toList());

    //We pick one hierarchy for now:
    LocationHierarchy locationHierarchy = plansAssignedToUser.get(0).getLocationHierarchy();
    List<LocationRelationship> locationRelationships = locationHierarchy.getLocationRelationships();

    List<LocationFacade> locationFacades = assignedLocations.stream().map(
        location -> LocationFacadeResponseFactory
            .fromLocationEntityAndLocationRelationship(location, locationRelationships)).collect(
        Collectors.toList());

    LocationTree locationTree = new LocationTree();
    locationTree.buildTreeFromList(locationFacades);

    LoginResponseData loginResponseData = LoginResponseData.builder().user(userFacadeResponse)
        .team(teamMember).jurisdictionIds(jurisdictionIds).jurisdictions(locationNames)
        .locations(locationTree)
        .build();
    return ResponseEntity.status(HttpStatus.OK).body(loginResponseData);
  }

  @GetMapping("rest/organization/user-assignment")
  public ResponseEntity<UserAssignmentResponse> getUserAssignedLocationsAndPlans() {
    User user = userService.getCurrentUser();
    List<Plan> plansAssignedToUser = userFacadeService.geAssignedPlans(user);
    Set<String> plans = plansAssignedToUser.stream().map(plan -> plan.getIdentifier().toString())
        .collect(
            Collectors.toSet());

    Set<Location> assignedLocations = new HashSet<>();
    plansAssignedToUser.forEach(plan -> assignedLocations.addAll(plan.getLocations()));

    Set<String> jurisdictionIds = assignedLocations.stream()
        .map(location -> location.getIdentifier().toString()).collect(
            Collectors.toSet());
    Optional<Organization> organizationOptional = user.getOrganizations().stream().findFirst();
    ; //Assumption that practioner always assigned to one org.
    if (organizationOptional.isEmpty()) {
      //handle case when user is not assgined
    }
    Organization organization = organizationOptional.get();
    Set<UUID> organizationIds = new HashSet<>();
    organizationIds.add(organization.getIdentifier());
    UserAssignmentResponse userAssignmentResponse = UserAssignmentResponse.builder().plans(plans)
        .jurisdictions(jurisdictionIds).organizationIds(organizationIds).build();
    return ResponseEntity.status(HttpStatus.OK).body(userAssignmentResponse);
  }
}
