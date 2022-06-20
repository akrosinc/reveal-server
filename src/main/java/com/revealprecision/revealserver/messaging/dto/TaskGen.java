package com.revealprecision.revealserver.messaging.dto;

import com.revealprecision.revealserver.enums.TaskProcessEnum;
import java.util.UUID;
import lombok.Data;

@Data
public class TaskGen {

  private UUID identifier;

  private TaskProcessEnum taskProcessEnum;

  private UUID baseEntityIdentifier;

  public TaskGen(UUID identifier, TaskProcessEnum taskProcessEnum){
    this.identifier = identifier;
    this.taskProcessEnum = taskProcessEnum;
  }
  public TaskGen(TaskProcessEnum taskProcessEnum,UUID baseEntityIdentifier){
    this.taskProcessEnum = taskProcessEnum;
    this.baseEntityIdentifier = baseEntityIdentifier;
  }
}
