package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.LookupTaskStatusResponse;
import com.revealprecision.revealserver.api.v1.dto.response.TaskResponse;
import com.revealprecision.revealserver.persistence.domain.Task;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

public class TaskResponseFactory {

  public static TaskResponse fromEntity(Task task) {
    return TaskResponse.builder().identifier(task.getIdentifier())

        .taskStatus(LookupTaskStatusResponse.builder()
            .identifier(task.getLookupTaskStatus().getIdentifier())
            .code(task.getLookupTaskStatus().getCode())
            .name(task.getLookupTaskStatus().getName())
            .build())
        .priority(task.getPriority())
        .authoredOn(task.getAuthoredOn()).description(task.getDescription())
        .lastModified(task.getModifiedDatetime())
        .executionPeriodStart(
            task.getExecutionPeriodStart())
        .executionPeriodEnd(
            task.getExecutionPeriodEnd())
        .actionIdentifier(task.getAction().getIdentifier())
        .build();
  }

  public static Page<TaskResponse> fromPageOfEntity(Page<Task> task) {
    return new PageImpl<>(
        task.stream().map(TaskResponseFactory::fromEntity).collect(Collectors.toList()));
  }
}
