package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.enums.BusinessStatusEnum;
import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import com.revealprecision.revealserver.enums.TaskStatusEnum;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskResponse {

  private UUID identifier;

  private String focus;

  private String code;

  private TaskStatusEnum status;

  private TaskPriorityEnum priority;

  private LocalDateTime authoredOn;

  private String description;

  private LocalDateTime lastModified;

  private BusinessStatusEnum businessStatus;

  private LocalDate executionPeriodStart;

  private LocalDate executionPeriodEnd;

  private String groupIdentifier;

  private UUID planIdentifier;

  private UUID intstantiatesUri;
}
