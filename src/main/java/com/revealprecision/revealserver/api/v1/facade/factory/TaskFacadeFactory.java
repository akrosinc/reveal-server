package com.revealprecision.revealserver.api.v1.facade.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.facade.models.Period;
import com.revealprecision.revealserver.api.v1.facade.models.TaskFacade;
import com.revealprecision.revealserver.api.v1.facade.models.TaskFacade.TaskPriority;
import com.revealprecision.revealserver.api.v1.facade.models.TaskFacade.TaskStatus;
import com.revealprecision.revealserver.api.v1.facade.util.DateTimeFormatter;
import com.revealprecision.revealserver.messaging.message.TaskEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskFacadeFactory {
  public static TaskFacade getTaskFacadeObj(String requester, String groupId, TaskEvent task, ObjectMapper objectMapper ) {
    TaskFacade build = TaskFacade.builder()
        .code(task.getAction().getTitle())
        .authoredOn(
            DateTimeFormatter.getDateTimeFacadeStringFromLocalDateTime(task.getAuthoredOn()))
        .description(task.getAction().getDescription())
        .executionPeriod(Period
            .between(DateTimeFormatter.getDateTimeFacadeStringFromLocalDateTime(
                    task.getExecutionPeriodStart().atStartOfDay())
                , DateTimeFormatter.getDateTimeFacadeStringFromLocalDateTime(
                    task.getExecutionPeriodEnd().atStartOfDay())))
        .focus(task.getAction().getIdentifier().toString())
        .forEntity(task.getBaseEntityIdentifier().toString())
        .identifier(task.getIdentifier().toString())
        .planIdentifier(task.getAction().getGoal().getPlan().getIdentifier().toString())
        .priority(TaskPriority.get(task.getPriority().name().toLowerCase()))
        .lastModified(
            DateTimeFormatter.getDateTimeFacadeStringFromLocalDateTime(task.getLastModified()))
        .status(TaskStatus.get(task.getLookupTaskStatus().getCode().toLowerCase()))
        .businessStatus(task.getBusinessStatus())
        .owner(task.getOwner())
        .requester(requester)
        .groupIdentifier(groupId)
        .structureId(task.getBaseEntityIdentifier().toString())
        .parentTaskId(task.getParentTaskId()!=null?task.getParentTaskId().toString():null)
        .serverVersion(task.getServerVersion() == null ? 0 : task.getServerVersion())
        .build();

    return build;
  }
}
