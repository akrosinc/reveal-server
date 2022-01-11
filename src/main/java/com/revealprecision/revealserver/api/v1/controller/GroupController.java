package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.persistence.domain.Group;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.service.GroupService;
import com.revealprecision.revealserver.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

  @Operation(summary = "Fetch a Plan by identfier",
      description = "Fetch a Plan by identfier",
      tags = {"Group"}
  )
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/group/{identifier}",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Group getPlanByIdentifier(
      @Parameter(description = "Group identifier") @PathVariable("identifier") UUID groupIdentifier) {
    return groupService.getGroupByIdentifier(groupIdentifier);
  }

  @Operation(summary = "Create a group",
      description = "Create a Group",
      tags = {"Group"}
  )
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(value = "/group", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Group createGroup(@Validated @RequestBody Group group) {
    return groupService.createGroup(group);
  }
}