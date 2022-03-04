package com.revealprecision.revealserver.api.v1.facade.controller;

import com.revealprecision.revealserver.api.v1.facade.factory.LoginResponseFactory;
import com.revealprecision.revealserver.api.v1.facade.factory.UserAssignmentResponseFactory;
import com.revealprecision.revealserver.api.v1.facade.models.LoginResponse;
import com.revealprecision.revealserver.api.v1.facade.models.UserAssignmentResponse;
import com.revealprecision.revealserver.api.v1.facade.service.UserFacadeService;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.service.UserService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserFacadeController {

  private final UserService userService;
  private final UserFacadeService userFacadeService;

  @GetMapping(value = "/security/authenticate", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<LoginResponse> authenticate() {
    User user = userService.getCurrentUser();
    Organization organization = userFacadeService.getOrganizationsAssignedToCurrentUser().stream().findFirst().get();
    Set<Plan> plansAssignedToUser = userFacadeService.getPlansAssignedToCurrentUser();
    Set<Location> assignedLocations = userFacadeService.getLocationsAssignedToCurrentUser();
    LoginResponse loginResponseData = LoginResponseFactory.fromEntities(user,organization,assignedLocations,plansAssignedToUser);
    return ResponseEntity.status(HttpStatus.OK).body(loginResponseData);
  }

  @GetMapping(value = "/rest/organization/user-assignment", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UserAssignmentResponse> getUserAssignedLocationsAndPlans() {
    Set<Plan> plansAssignedToUser = userFacadeService.getPlansAssignedToCurrentUser();
    Set<Location> assignedLocations = userFacadeService.getLocationsAssignedToCurrentUser();
    Set<Organization> organizations = userFacadeService.getOrganizationsAssignedToCurrentUser();
    return ResponseEntity.status(HttpStatus.OK)
        .body(UserAssignmentResponseFactory.fromEntities(plansAssignedToUser,
            organizations, assignedLocations));
  }
}
