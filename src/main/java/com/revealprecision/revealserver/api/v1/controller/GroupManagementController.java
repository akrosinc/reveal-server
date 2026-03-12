package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.GroupResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.GroupManagementRequest;
import com.revealprecision.revealserver.api.v1.dto.response.GroupManagementResponse;
import com.revealprecision.revealserver.api.v1.dto.response.GroupResponse;
import com.revealprecision.revealserver.enums.SummaryEnum;
import com.revealprecision.revealserver.persistence.projection.GroupManagementProjection;
import com.revealprecision.revealserver.service.GroupManagementService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
}
