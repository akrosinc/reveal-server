package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.request.AssignLocationsToTeamRequest;
import com.revealprecision.revealserver.api.v1.dto.request.GlobalUserRequest;
import com.revealprecision.revealserver.api.v1.dto.request.GroupManagementRequest;
import com.revealprecision.revealserver.api.v1.dto.request.OrganizationRoleRequest;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.GroupManagementResponse;
import com.revealprecision.revealserver.api.v1.dto.response.IdentifierNameResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationHierarchyResponse;
import com.revealprecision.revealserver.persistence.projection.GroupManagementProjection;
import com.revealprecision.revealserver.service.GroupManagementService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groupmanagement")
@RequiredArgsConstructor
public class GroupManagementController {
  private final GroupManagementService groupManagementService;

  @Operation(summary = "Fetch all management groups", description = "Fetch all management Groups", tags = {"GroupManagement"})
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Page<GroupManagementProjection>> getGroups(Pageable pageable) {
    return ResponseEntity.ok(groupManagementService.getGroups(pageable));
  }

  @Operation(summary = "Create a management group", description = "Create a Management Group", tags = {"GroupManagement"})
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> createGroup(@RequestBody GroupManagementRequest groupManagementGroupRequest) {
    groupManagementService.createGroup(groupManagementGroupRequest);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }


  @Operation(summary = "Get management group By Id", description = "Get management group By Id", tags = {"GroupManagement"})
  @GetMapping(path = "/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<GroupManagementResponse> getGroupById(@PathVariable UUID identifier) {
    return ResponseEntity.ok(groupManagementService.getGroupById(identifier));
  }

  @Operation(
      summary = "Update a management group",
      description = "Update a Management Group",
      tags = {"GroupManagement"})
  @PutMapping(value = "/{identifier}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> updateGroup(
      @PathVariable UUID identifier,
      @RequestBody GroupManagementRequest request) {
    groupManagementService.updateGroup(identifier, request);
    return ResponseEntity.ok().build();
  }


  @Operation(summary = "Fetch all locations associated to users", description = "Fetch all locations associated to users", tags = {"GroupManagement"})
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE , path = "/user/{userId}/locationstree")
  public ResponseEntity<List<GeoTreeResponse>> getUserLocations(@PathVariable UUID userId) {
    return ResponseEntity.ok(groupManagementService.getUserLocations(userId));
  }

  @Operation(summary = "Fetch all groups associated to users", description = "Fetch all groups associated to users", tags = {"GroupManagement"})
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE , path = "/user/{userId}/groups")
  public ResponseEntity<List<String>> getUserGroups(@PathVariable UUID userId) {
    return ResponseEntity.ok(groupManagementService.getUserGroups(userId));
  }

  @Operation(summary = "Fetch all datasets tags associated to users", description = "Fetch all datasets tags  associated to users", tags = {"GroupManagement"})
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE , path = "/user/{userId}/datasettags")
  public ResponseEntity<List<String>> getUserDatasetTags(@PathVariable UUID userId) {
    return ResponseEntity.ok(groupManagementService.getUserDatasetTags(userId));
  }

  @Operation(summary = "Assign Location to Group(Team)", description = "Assign Location to Group(Team)", tags = {"GroupManagement"})
  @PostMapping(path = "/assignlocation", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> assignLocations(@RequestBody AssignLocationsToTeamRequest assignLocationsToTeamRequest) {
    groupManagementService.assignLocations(assignLocationsToTeamRequest);
    return ResponseEntity.ok().build();
  }


  @Operation(summary = "Fetch groups and location assignment of instance", description = "Fetch groups and location assignment of instance", tags = {"GroupManagement"})
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE , path = "/instance/locationassigments")
  public ResponseEntity<LocationHierarchyResponse> getInstanceGroupsLocations() {
    return ResponseEntity.ok(groupManagementService.getInstanceGroupsLocationsTree());
  }

  @Operation(
      summary = "Get all group roles",
      description = "Get all available group roles",
      tags = {"GroupManagement"})
  @GetMapping("/roles/list")
  public ResponseEntity<List<IdentifierNameResponse>> getGroupRoles() {
    return ResponseEntity.status(HttpStatus.OK).body(groupManagementService.getGroupRoles());
  }

  @Operation(summary = "Create organization role", tags = {"GroupManagement"})
  @PostMapping(value = "/roles", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<IdentifierNameResponse> createOrganizationRole(
      @RequestBody @Valid OrganizationRoleRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(groupManagementService.createGroupRole(request));
  }

  @Operation(summary = "Update organization role", tags = {"GroupManagement"})
  @PutMapping(value = "/roles/{identifier}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<IdentifierNameResponse> updateOrganizationRole(
      @PathVariable UUID identifier,
      @RequestBody @Valid OrganizationRoleRequest request) {
    return ResponseEntity.ok(groupManagementService.updateGroupRole(identifier, request));
  }

  @Operation(summary = "Delete organization role", tags = {"GroupManagement"})
  @DeleteMapping(value = "/roles/{identifier}")
  public ResponseEntity<Void> deleteOrganizationRole(@PathVariable UUID identifier) {
    groupManagementService.deleteGroupRole(identifier);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Add organization user", tags = {"GroupManagement"})
  @PostMapping(value = "/roles")
  public ResponseEntity<Void> addUser(
      @RequestBody @Valid GlobalUserRequest request) {
    groupManagementService.addUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @Operation(  summary = "Fetch all management groups teams", description = "Fetch all management Groups teams", tags = {"GroupManagement"})
  @GetMapping(path = "/bytype/teams", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<GroupManagementProjection>> getGroupsTeams() {
    return ResponseEntity.ok(groupManagementService.getGroupsTeams());
  }
}
