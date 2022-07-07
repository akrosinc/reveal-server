package com.revealprecision.revealserver.api.v1.facade.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.api.v1.dto.request.PersonRequest;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Data
@Setter
@Getter
@Builder
@FieldNameConstants
@JsonInclude(value = Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class TaskFacade implements Serializable {

  private static final long serialVersionUID = -9118755114172291102L;

  public enum TaskStatus {

    @JsonProperty("Draft")
    DRAFT,
    @JsonProperty("Ready")
    READY,
    @JsonProperty("Cancelled")
    CANCELLED,
    @JsonProperty("In Progress")
    IN_PROGRESS,
    @JsonProperty("Completed")
    COMPLETED,
    @JsonProperty("Failed")
    FAILED,
    @JsonProperty("Archived")
    ARCHIVED;

    private static final Map<String, TaskStatus> lookup = new HashMap<>();

    static {
      lookup.put("draft", TaskStatus.DRAFT);
      lookup.put("ready", TaskStatus.READY);
      lookup.put("cancelled", TaskStatus.CANCELLED);
      lookup.put("inprogress", TaskStatus.IN_PROGRESS);
      lookup.put("completed", TaskStatus.COMPLETED);
      lookup.put("failed", TaskStatus.FAILED);
      lookup.put("archived", TaskStatus.ARCHIVED);
    }

    public static TaskStatus get(String algorithm) {
      return lookup.get(algorithm);
    }
  }

  public enum TaskPriority {

    @JsonProperty("stat")
    STAT,
    @JsonProperty("asap")
    ASAP,
    @JsonProperty("urgent")
    URGENT,
    @JsonProperty("routine")
    ROUTINE;

    public static TaskPriority get(String priority) {
      if (priority == null) {
        throw new IllegalArgumentException("Value is required");
      }
      switch (priority) {
        case "routine":
          return ROUTINE;
        case "urgent":
          return URGENT;
        case "asap":
          return ASAP;
        case "stat":
          return STAT;
        default:
          throw new IllegalArgumentException("Not a valid Task priority");
      }

    }
  }

  public static final String[] INACTIVE_TASK_STATUS = new String[]{TaskStatus.CANCELLED.name(),
      TaskStatus.ARCHIVED.name()};

  private String identifier;

  private String planIdentifier;

  private String groupIdentifier;

  private TaskStatus status;

  private String businessStatus;

  private TaskPriority priority;

  private String code;

  private String description;

  private String focus;

  @JsonProperty("for")
  private String forEntity;

  private PersonRequest personRequest;

  private LocationRequest locationRequest;

  private Period executionPeriod;

  private String authoredOn;

  private String lastModified;

  private String owner;

  private long serverVersion;

  private String reasonReference;

  private String location;

  private String requester;

  private String syncStatus;

  private String structureId;

  private Restriction restriction;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Restriction {

    private int repetitions;

    private Period period;
  }
}

