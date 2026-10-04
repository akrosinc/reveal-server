package com.revealprecision.revealserver.messaging.dto;

import com.revealprecision.revealserver.enums.TaskProcessEnum;
import java.util.UUID;
import lombok.Data;

@Data
public class TaskGen {

  private UUID identifier;

  private TaskProcessEnum taskProcessEnum;

  private UUID baseEntityIdentifier;

  private UUID locationIdentifier;

  public TaskGen(UUID identifier, TaskProcessEnum taskProcessEnum){
    this.identifier = identifier;
    this.taskProcessEnum = taskProcessEnum;
  }
  public TaskGen(TaskProcessEnum taskProcessEnum,UUID baseEntityIdentifier){
    this.taskProcessEnum = taskProcessEnum;
    this.baseEntityIdentifier = baseEntityIdentifier;
  }

  /**
   * Builds a generate candidate for an entity_data task. {@code baseEntityIdentifier} is set to the
   * entity_data identifier so the resulting task stores it, while {@code locationIdentifier} is the
   * inherited (parent) location grounding.
   */
  public static TaskGen forEntityData(UUID entityDataIdentifier, UUID locationIdentifier) {
    TaskGen taskGen = new TaskGen(TaskProcessEnum.GENERATE, entityDataIdentifier);
    taskGen.setLocationIdentifier(locationIdentifier);
    return taskGen;
  }
}
