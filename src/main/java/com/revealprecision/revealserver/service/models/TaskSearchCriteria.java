package com.revealprecision.revealserver.service.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.revealprecision.revealserver.enums.TaskStatusEnum;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class TaskSearchCriteria {

  private UUID planIdentifier;

  private TaskStatusEnum taskStatus;

  private String code;

  private String groupIdentifier;
}
