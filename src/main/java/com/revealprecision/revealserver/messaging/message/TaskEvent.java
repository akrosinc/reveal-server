package com.revealprecision.revealserver.messaging.message;

import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TaskEvent  extends Message  {

  private UUID identifier;

  private LookupTaskStatusEvent lookupTaskStatus;

  private String locationName;

  private List<String> baseLocationIds;

  private String ownerId;
  private String owner;

  private ActionEvent action;

  private TaskPriorityEnum priority;

  private LocalDateTime authoredOn;

  private String description;

  private LocalDateTime lastModified;

  private String personId;

  private String locationId;

  private UUID baseEntityIdentifier;

   private Long serverVersion;

  private String businessStatus;

  private LocalDate executionPeriodStart;

  private LocalDate executionPeriodEnd;

  private List<UUID> ancestors;

  private UUID flattenedLocationAncestor;
}
