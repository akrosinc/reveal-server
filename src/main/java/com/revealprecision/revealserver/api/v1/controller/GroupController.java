package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.GroupResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.GroupRequest;
import com.revealprecision.revealserver.api.v1.dto.response.GroupResponse;
import com.revealprecision.revealserver.persistence.domain.Group;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.service.GroupService;
import com.revealprecision.revealserver.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/")
public class GroupController {

  private final GroupService groupService;

  @Autowired
  public GroupController(GroupService groupService) {
    this.groupService = groupService;
  }

  @Operation(summary = "Fetch all groups",
      description = "Fetch all Groups",
      tags = {"Group"}
  )
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/group",
      produces = "application/json"
  )
  public Page<Group> getGroups(
      @Parameter(description = "Page number to return") @RequestParam(defaultValue = "0", required = false) Integer pageNumber,
      @Parameter(description = "Number of records per page") @RequestParam(defaultValue = "50", required = false) Integer pageSize) {
    return groupService.getGroups(pageNumber, pageSize);
  }

  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/groups",
      produces = "application/json"
  )
  public List<Group> getGroups() {
    return groupService.getAllGroups();
  }


  @Operation(summary = "Fetch a group by identfier",
      description = "Fetch a group by identfier",
      tags = {"Group"}
  )
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/group/{identifier}",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public GroupResponse getGroupByIdentifier(
      @Parameter(description = "Group identifier") @PathVariable("identifier") UUID groupIdentifier
  ,@RequestParam(name = "_summary",required = false) boolean showSummary) {
    return GroupResponseFactory.fromEntity(groupService.getGroupByIdentifier(groupIdentifier),showSummary);
  }

  @Operation(summary = "Create a group",
      description = "Create a Group",
      tags = {"Group"}
  )
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(value = "/group", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public GroupResponse createGroup(@Validated @RequestBody GroupRequest groupRequest) {
    return GroupResponseFactory.fromEntity(groupService.createGroup(groupRequest),false);
  }

  @Operation(summary = "Delete a group by identfier",
      description = "Delete a group by identfier",
      tags = {"Group"}
  )
  @DeleteMapping(value = "/group/{identifier}",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<?> removeGroupByIdentifier(
      @Parameter(description = "Group identifier") @PathVariable("identifier") UUID groupIdentifier) {
    groupService.removeGroup(groupIdentifier);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Update a group by identfier",
      description = "Update a group by identfier",
      tags = {"Group"}
  )
  @PutMapping(value = "/group/{identifier}",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public GroupResponse updateGroupByIdentifier(
      @Parameter(description = "Group identifier") @PathVariable("identifier") UUID groupIdentifier,
      @Validated @RequestBody GroupRequest groupRequest) {
    return GroupResponseFactory.fromEntity(groupService.updateGroup(groupIdentifier, groupRequest),false);
  }

}