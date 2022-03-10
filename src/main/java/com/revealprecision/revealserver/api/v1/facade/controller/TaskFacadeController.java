package com.revealprecision.revealserver.api.v1.facade.controller;

import com.revealprecision.revealserver.api.v1.facade.models.TaskDto;
import com.revealprecision.revealserver.api.v1.facade.models.TaskFacade;
import com.revealprecision.revealserver.api.v1.facade.request.TaskSyncRequestWrapper;
import com.revealprecision.revealserver.api.v1.facade.service.TaskFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
@Slf4j
public class TaskFacadeController {

  public static final String TOTAL_RECORDS = "total_records";
  public final TaskFacadeService taskFacadeService;

  @Autowired
  public TaskFacadeController(TaskFacadeService taskFacadeService) {
    this.taskFacadeService = taskFacadeService;
  }

  @Operation(summary = "Facade for Android Task Resource", description = "Sync Tasks", tags = {
      "Task"})
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/task/v2/sync", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Transactional
  public ResponseEntity<List<TaskFacade>> taskSync(
      @RequestBody TaskSyncRequestWrapper taskSyncRequestWrapper) {

    String plan = StringUtils.join(taskSyncRequestWrapper.getPlan(), ",");
    String group = StringUtils.join(taskSyncRequestWrapper.getGroup(), ",");
    boolean returnCount = taskSyncRequestWrapper.isReturnCount();

    List<TaskFacade> taskFacades = taskFacadeService.syncTasks(plan, group);

    if (returnCount) {
      HttpHeaders headers = new HttpHeaders();
      headers.add(TOTAL_RECORDS, String.valueOf(taskFacades.size()));
      return ResponseEntity.ok().headers(headers).body(taskFacades);
    } else {
      return ResponseEntity.ok().body(taskFacades);
    }

  }

  @Operation(summary = "Facade for Android Task Resource", description = "Sync Tasks", tags = {
      "Task"})
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(value = "/v2/task/add", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> batchSave(@RequestBody List<TaskDto> tasks) {


    List<TaskDto> taskDtosUnprocessed = taskFacadeService.addTasks(tasks);

    if (taskDtosUnprocessed.isEmpty()) {
      return new ResponseEntity<>("All Tasks  processed", HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(
          "Tasks with identifiers not processed: " + taskDtosUnprocessed.stream()
              .map(TaskDto::getIdentifier).collect(
                  Collectors.joining(",")),
          HttpStatus.CREATED);
    }

  }

}
