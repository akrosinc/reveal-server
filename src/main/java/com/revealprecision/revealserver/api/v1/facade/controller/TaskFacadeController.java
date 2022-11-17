package com.revealprecision.revealserver.api.v1.facade.controller;

import com.revealprecision.revealserver.api.v1.facade.models.TaskDto;
import com.revealprecision.revealserver.api.v1.facade.models.TaskFacade;
import com.revealprecision.revealserver.api.v1.facade.models.TaskUpdateFacade;
import com.revealprecision.revealserver.api.v1.facade.request.TaskSyncRequest;
import com.revealprecision.revealserver.api.v1.facade.service.TaskFacadeService;
import com.revealprecision.revealserver.util.UserUtils;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/v2/task")
@Slf4j
public class TaskFacadeController {

  public static final String TOTAL_RECORDS = "total_records";
  public final TaskFacadeService taskFacadeService;

  @Autowired
  public TaskFacadeController(TaskFacadeService taskFacadeService) {
    this.taskFacadeService = taskFacadeService;
  }

  @Operation(summary = "Facade for Android Task Resource", description = "Sync Tasks", tags = {
      "Task-Facade"})
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/sync", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Transactional
  public ResponseEntity<List<TaskFacade>> taskSync(@RequestBody TaskSyncRequest taskSyncRequest) {

    List<UUID> jurisdictionIdentifiers = taskSyncRequest.getGroup();
    boolean returnCount = taskSyncRequest.isReturnCount();
    Long serverVersion = taskSyncRequest.getServerVersion();
    if (serverVersion == null) {
      serverVersion = 0L;
    }

    List<TaskFacade> taskFacades = taskFacadeService.syncTasks(taskSyncRequest.getPlan(),
        jurisdictionIdentifiers, serverVersion, UserUtils.getCurrentPrincipleName());

    if (returnCount) {
      HttpHeaders headers = new HttpHeaders();
      headers.add(TOTAL_RECORDS, String.valueOf(taskFacades.size()));
      return ResponseEntity.ok().headers(headers).body(taskFacades);
    } else {
      return ResponseEntity.ok().body(taskFacades);
    }

  }

  @Operation(summary = "Facade for Android Task Resource", description = "Sync Tasks", tags = {
      "Task-Facade"})
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(value = "/add", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> batchSave(@RequestBody List<TaskDto> taskDtos) {

       List<TaskDto> taskDtosUnprocessed = taskFacadeService.addTaskDtos(taskDtos);

    if (taskDtosUnprocessed.isEmpty()) {
      return new ResponseEntity<>("All Tasks  processed", HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(
          "Tasks with identifiers not processed: " + taskDtosUnprocessed.stream()
              .map(TaskDto::getIdentifier).collect(Collectors.joining(",")), HttpStatus.CREATED);
    }

  }

  @Operation(summary = "Facade for Android Task Resource", description = "Update Status for Tasks", tags = {
      "Task-Facade"})
  @RequestMapping(value = "/update_status", method = RequestMethod.POST, consumes = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
  public ResponseEntity<String> updateStatus(@RequestBody List<TaskUpdateFacade> taskUpdates) {
    List<String> updateTasks = taskFacadeService
        .updateTaskStatusAndBusinessStatusForListOfTasks(taskUpdates);

    JSONObject json = new JSONObject();
    if (updateTasks.size() > 0) {
      json.put("task_ids", updateTasks);
    } else {
      return new ResponseEntity<>("Tasks not Updated: ", HttpStatus.CREATED);
    }
    return new ResponseEntity<>(json.toString(), HttpStatus.CREATED);
  }
}