package com.revealprecision.revealserver.api.v1.facade;

import com.revealprecision.revealserver.api.v1.facade.models.TaskFacade;
import com.revealprecision.revealserver.api.v1.facade.request.TaskSyncRequestWrapper;
import com.revealprecision.revealserver.api.v1.facade.service.TaskFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

  @Autowired
  TaskFacadeService taskFacadeService;

  @Operation(summary = "Create a task", description = "Create a Task", tags = {"Task"})
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(value = "/task/v2/sync", consumes = "application/json", produces = "application/json")
  @Transactional
  public ResponseEntity<List<TaskFacade>> taskSync(@RequestBody TaskSyncRequestWrapper taskSyncRequestWrapper) {

    String plan = StringUtils.join(taskSyncRequestWrapper.getPlan(), ",");
    String group = StringUtils.join(taskSyncRequestWrapper.getGroup(), ",");
    boolean returnCount = taskSyncRequestWrapper.isReturnCount();

    List<TaskFacade> taskFacades = taskFacadeService.syncTasks(plan, group);

    if (returnCount) {
      HttpHeaders headers = new HttpHeaders();
      headers.add("total_records", String.valueOf(taskFacades.size()));
      return ResponseEntity.ok().headers(headers).body(taskFacades);
    }else{
      return ResponseEntity.ok().body(taskFacades);
    }

  }

}
