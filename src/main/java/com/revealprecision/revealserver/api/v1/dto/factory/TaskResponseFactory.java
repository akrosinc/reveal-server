package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.TaskResponse;
import com.revealprecision.revealserver.persistence.domain.Task;
import java.time.ZoneId;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

public class TaskResponseFactory {
public static TaskResponse fromEntity(Task task) {
  return TaskResponse.builder()
      .identifier(task.getIdentifier())
      .intstantiatesUri(task.getInstantiatesUriForm().getIdentifier())
      .focus(task.getFocus())
      .code(task.getCode()).status(task.getStatus())
      .priority(task.getPriority()).authoredOn(task.getAuthoredOn())
      .description(task.getDescription()).lastModified(task.getLastModified())
      .businessStatus(task.getBusinessStatus()).executionPeriodStart(
          task.getExecutionPeriodStart().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
      .executionPeriodEnd(
              task.getExecutionPeriodEnd().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
      .groupIdentifier(task.getGroupIdentifier()).planIdentifier(task.getPlan().getIdentifier()).build();
}

  public static Page<TaskResponse> fromPageOfEntity(Page<Task> task) {

    return new PageImpl<>(
        task.stream().map(TaskResponseFactory::fromEntity).collect(Collectors.toList()));
  }
}
