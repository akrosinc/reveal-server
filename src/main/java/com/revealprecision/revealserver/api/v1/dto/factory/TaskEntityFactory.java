package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.TaskRequest;
import com.revealprecision.revealserver.persistence.domain.Form;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Task;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class TaskEntityFactory {
public static Task entityFromRequestObj(TaskRequest taskRequest, Form form, Plan plan) {
  return Task.builder().instantiatesUriForm(form).focus(taskRequest.getFocus())
      .code(taskRequest.getCode()).status(taskRequest.getStatus())
      .priority(taskRequest.getPriority()).authoredOn(LocalDateTime.now())
      .description(taskRequest.getDescription()).lastModified(LocalDateTime.now())
      .businessStatus(taskRequest.getBusinessStatus()).executionPeriodStart(Date.from(
          taskRequest.getExecutionPeriodStart().atStartOfDay(ZoneId.systemDefault()).toInstant()))
      .executionPeriodEnd(Date.from(
          taskRequest.getExecutionPeriodStart().atStartOfDay(ZoneId.systemDefault()).toInstant()))
      .groupIdentifier(taskRequest.getGroupIdentifier()).plan(plan).build();
}
}
