package com.revealprecision.revealserver.persistence.domain;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.GenericGenerator;

@FieldNameConstants
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskBusinessStateTracker {

  @Id
  @GeneratedValue(generator = "custom-generator")
  @GenericGenerator(name = "custom-generator", strategy = "com.revealprecision.revealserver.persistence.generator.CustomIdentifierGenerator")
  private UUID identifier;
  private UUID parentLocationIdentifier;
  private String parentLocationName;
  private String parentGeographicLevelName;

  private UUID planIdentifier;
  private UUID locationHierarchyIdentifier;

  private UUID taskLocationIdentifier;
  private String taskLocationGeographicLevelName;
  private String taskLocationName;
  private String taskBusinessStatus;

  private LocalDateTime taskBusinessStatusUpdateTime;
  public TaskBusinessStateTracker(UUID taskLocationIdentifier,
      String taskLocationGeographicLevelName,
      String taskLocationName,
      String taskBusinessStatus){
    this.taskLocationIdentifier = taskLocationIdentifier;
    this.taskLocationGeographicLevelName = taskLocationGeographicLevelName;
    this.taskLocationName = taskLocationName;
    this.taskBusinessStatus = taskBusinessStatus;
  }
}
