package com.revealprecision.revealserver.api.v1.facade.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationHierarchyResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationHierarchyResponse;
import com.revealprecision.revealserver.api.v1.facade.dto.factory.TeamMemberResponseFactory;
import com.revealprecision.revealserver.api.v1.facade.dto.factory.TeamResponseFactory;
import com.revealprecision.revealserver.api.v1.facade.dto.factory.UserFacadeResponseFactory;
import com.revealprecision.revealserver.api.v1.facade.dto.response.LoginResponseData;
import com.revealprecision.revealserver.api.v1.facade.dto.service.UserFacadeService;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.service.UserService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserFacadeController {

  private final UserService userService;
  private final UserFacadeService userFacadeService;

  @RequestMapping("/security/authenticate")
  public ResponseEntity<LoginResponseData> authenticate() {
    User user = userService.getCurrentUser();
    Optional<Organization> organizationOptional = user.getOrganizations().stream().findFirst();; //Assumption that practioner always assigned to one org.
    if(organizationOptional.isEmpty()){
      //handle case when user is not assgined
    }
    Organization organization = organizationOptional.get();
    var userFacadeResponse = UserFacadeResponseFactory.fromEntity(user);
    var team = TeamResponseFactory.fromEntity(organization);
    var teamMember = TeamMemberResponseFactory.fromEntities(organization,user);
    teamMember.setTeam(team);

    List<Plan> plansAssignedToUser = userFacadeService.geAssignedPlans(user);
    Set<Location> assignedLocationSet = new HashSet<>();
    plansAssignedToUser.forEach(plan -> assignedLocationSet.addAll(plan.getLocations()));

    Set<String> jurisdictionIds = assignedLocationSet.stream().map(location -> location.getIdentifier().toString()).collect(
        Collectors.toSet());
    List<String> locationNames = assignedLocationSet.stream().map(location -> location.getName()).collect(
        Collectors.toList());


    //We pick one hierarchy for now:
    LocationHierarchy locationHierarchy = plansAssignedToUser.get(0).getLocationHierarchy();
    LocationHierarchyResponse locationHierarchyResponse = LocationHierarchyResponseFactory.fromEntityWithTree(locationHierarchy,true)
    List<GeoTreeResponse> geoTreeResponses = locationHierarchyResponse.getGeoTree();

    //TODO: Now we will map the locationHierarchyResponse tree to match LocationTree



    LoginResponseData loginResponseData = LoginResponseData.builder().user(userFacadeResponse)
        .team(teamMember).jurisdictionIds(jurisdictionIds).jurisdictions(locationNames).locations(null)
        .build();
    return ResponseEntity.status(HttpStatus.OK).body(loginResponseData);
  }
}
