package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.LookupTaskStatusResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.TaskResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.TaskCreateRequest;
import com.revealprecision.revealserver.api.v1.dto.request.TaskUpdateRequest;
import com.revealprecision.revealserver.api.v1.dto.response.CountResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LookupTaskStatusResponse;
import com.revealprecision.revealserver.api.v1.dto.response.TaskResponse;
import com.revealprecision.revealserver.service.TaskService;
import com.revealprecision.revealserver.service.models.TaskSearchCriteria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/task/")
@Slf4j
public class TaskController {

  private final TaskService taskService;

  @Autowired
  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @Operation(summary = "Search for Tasks", description = "Search for Tasks", tags = {"Task"})
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Page<TaskResponse>> getTasks(@Nullable TaskSearchCriteria search,
       Pageable pageable) {

    if (search != null) {
      return ResponseEntity.status(HttpStatus.OK)
          .body(TaskResponseFactory.fromPageOfEntity(taskService.searchTasks(search, pageable)));
    } else {
      return ResponseEntity.status(HttpStatus.OK)
          .body(TaskResponseFactory.fromPageOfEntity(taskService.getTasks(pageable)));
    }
  }

  @Operation(summary = "Search for Tasks", description = "Search for Tasks", tags = {"Task"})
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, params = {"_summary=COUNT"})
  public ResponseEntity<CountResponse> getTaskCount(@Nullable TaskSearchCriteria search) {

    if (search != null) {
      return ResponseEntity.status(HttpStatus.OK)
          .body(new CountResponse(taskService.countTasksBySearchCriteria(search)));
    } else {
      return ResponseEntity.status(HttpStatus.OK)
          .body(new CountResponse(taskService.getAllTaskCount()));
    }
  }

  @Operation(summary = "Retrieve all Task Statuses", description = "Retrieve all Task Statuses", tags = {
      "Task"})
  @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<LookupTaskStatusResponse>> getAllTaskStatuses() {

    return ResponseEntity.status(HttpStatus.OK).body(
        LookupTaskStatusResponseFactory.fromEntityList(taskService.getAllTaskStatus()));

  }

  @Operation(summary = "Fetch a Task by identifier", description = "Fetch a Task by identifier", tags = {
      "Task"})
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
  public TaskResponse getTaskByIdentifier(
      @Parameter(description = "Task identifier") @PathVariable("identifier") UUID taskIdentifier) {
    return TaskResponseFactory.fromEntity(taskService.getTaskByIdentifier(taskIdentifier));
  }

  @Operation(summary = "Create a task", description = "Create a Task", tags = {"Task"})
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public TaskResponse createTask(@Valid @RequestBody TaskCreateRequest taskRequest) {
    return TaskResponseFactory.fromEntity(taskService.createTask(taskRequest));
  }

  @Operation(summary = "Update a task", description = "Update a Task", tags = {"Task"})
  @ResponseStatus(HttpStatus.CREATED)
  @PutMapping(value = "/{identifier}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public TaskResponse updateTask(
      @Parameter(description = "GUID task identifier") @PathVariable("identifier") UUID identifier,
      @Valid @RequestBody TaskUpdateRequest taskUpdateRequest) {
    return TaskResponseFactory.fromEntity(taskService.updateTask(identifier, taskUpdateRequest));
  }

}