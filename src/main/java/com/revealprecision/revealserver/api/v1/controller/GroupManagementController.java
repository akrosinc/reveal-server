package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.GroupResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.GroupManagementRequest;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.GroupManagementResponse;
import com.revealprecision.revealserver.api.v1.dto.response.GroupResponse;
import com.revealprecision.revealserver.enums.SummaryEnum;
import com.revealprecision.revealserver.persistence.projection.GroupManagementProjection;
import com.revealprecision.revealserver.service.GroupManagementService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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


  @Operation(summary = "Fetch all locations associated to users", description = "Fetch all locations associated to users", tags = {"GroupManagement"})
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE , path = "user//{userId}/locationstree")
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
}
