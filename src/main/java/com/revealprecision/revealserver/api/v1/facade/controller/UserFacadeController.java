package com.revealprecision.revealserver.api.v1.facade.controller;

import com.revealprecision.revealserver.api.v1.facade.factory.LoginResponseFactory;
import com.revealprecision.revealserver.api.v1.facade.factory.UserAssignmentResponseFactory;
import com.revealprecision.revealserver.api.v1.facade.models.LoginResponse;
import com.revealprecision.revealserver.api.v1.facade.models.UserAssignmentResponse;
import com.revealprecision.revealserver.api.v1.facade.service.UserFacadeService;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
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
  private final LocationRelationshipService locationRelationshipService;

  @Operation(summary = "Used by reveal-client to get UserContext", description = "Returns LoginResponse which contains user details, LocationTree, Team details,assigned jurisdictions", tags = {
      "UserContext"})
  @GetMapping(value = "/security/authenticateold", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<LoginResponse> authenticate() {
    User user = userService.getCurrentUser();
    Organization organization = userFacadeService.getOrganizationsAssignedToCurrentUser().stream()
        .findFirst().get();
    Map<Plan, Set<Location>> assignedLocationsPerPlan = userFacadeService.getLocationsAssignedToCurrentUser();
    LoginResponse loginResponse = LoginResponseFactory
        .fromEntities(user, organization, assignedLocationsPerPlan);
    return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
  }

  @Operation(summary = "Used by reveal-client to get UserContext", description = "Returns LoginResponse which contains user details, LocationTree, Team details,assigned jurisdictions", tags = {
      "UserContext"})
  @GetMapping(value = "/security/authenticate", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<LoginResponse> authenticate2() {
    User user = userService.getCurrentUser();
    Organization organization = userFacadeService.getOrganizationsAssignedToCurrentUser().stream()
        .findFirst().get();
    Map<Plan, Set<Location>> assignedLocationsPerPlan = userFacadeService.getLocationsAssignedToCurrentUser();

    Set<Location> assignedLocations = assignedLocationsPerPlan.values().stream()
        .flatMap(Collection::stream)
        .map(location -> Location.builder()
            .identifier(location.getIdentifier())
            .name(location.getName())
            .geographicLevel(location.getGeographicLevel())
            .serverVersion(location.getServerVersion())
            .build())
        .collect(Collectors.toSet());

    Map<Plan, List<LocationRelationship>> locationRelationshipMap = assignedLocationsPerPlan.entrySet().stream().map(entry ->
        new SimpleEntry<>(entry.getKey(),locationRelationshipService.getLocationRelationshipsForLocations(
            entry.getKey().getLocationHierarchy().getIdentifier(),
            assignedLocations.stream().map(Location::getIdentifier).collect(
                Collectors.toList()))
    )).collect(Collectors.toMap(Entry::getKey,Entry::getValue));

    LoginResponse loginResponse = LoginResponseFactory
        .fromEntities2(user, organization, assignedLocationsPerPlan, assignedLocations, locationRelationshipMap);
    return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
  }

  @Operation(summary = "Used by reveal-client to get data that is assigned to User", description = "Used by reveal-client to get data that is assigned to User", tags = {
      "UserAssignment"})
  @GetMapping(value = "/rest/organization/user-assignment", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UserAssignmentResponse> getUserAssignedLocationsAndPlans() {
    Map<Plan, Set<Location>> assignedLocationsPerPlan = userFacadeService.getLocationsAssignedToCurrentUser();
    Set<Organization> organizations = userFacadeService.getOrganizationsAssignedToCurrentUser();
    return ResponseEntity.status(HttpStatus.OK)
        .body(UserAssignmentResponseFactory.fromEntities(
            organizations, assignedLocationsPerPlan));
  }
}
