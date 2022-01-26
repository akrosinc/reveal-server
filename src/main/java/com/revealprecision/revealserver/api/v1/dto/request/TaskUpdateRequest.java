package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import com.revealprecision.revealserver.persistence.domain.LookupTaskStatus;
import java.time.LocalDate;
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
public class TaskUpdateRequest {

  private TaskPriorityEnum priority;

  private String description;

  private UUID lookupTaskStatus;

  private LocalDate executionPeriodStart;

  private LocalDate executionPeriodEnd;

}
