package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.TaskCreateRequest;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.LookupTaskStatus;
import com.revealprecision.revealserver.persistence.domain.Task;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class TaskEntityFactory {

  public static Task entityFromRequestObj(TaskCreateRequest taskRequest, Action action,
      LookupTaskStatus lookupTaskStatus) {
    return Task.builder()
        .lookupTaskStatus(lookupTaskStatus)
        .priority(taskRequest.getPriority()).authoredOn(LocalDateTime.now())
        .description(taskRequest.getDescription())
        .action(action)
        .executionPeriodStart(
            taskRequest.getExecutionPeriodStart())
        .executionPeriodEnd(
            taskRequest.getExecutionPeriodStart())
        .build();
  }
}
