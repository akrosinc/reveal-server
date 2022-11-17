package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.facade.models.TaskUpdateFacade;
import com.revealprecision.revealserver.persistence.projection.TaskDataFromEventProjection;

public class TaskUpdateFacadeFactory {

  public static  TaskUpdateFacade fromTaskDataFromEvent(TaskDataFromEventProjection taskDataFromEventProjection){
    return TaskUpdateFacade.builder()
        .status(taskDataFromEventProjection.getTaskStatus())
        .businessStatus(taskDataFromEventProjection.getEventBusinessStatus())
        .identifier(taskDataFromEventProjection.getTaskIdentifier())
        .build();
  }
}
