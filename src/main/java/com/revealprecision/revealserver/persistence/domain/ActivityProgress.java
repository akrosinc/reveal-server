package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.enums.ActivityProgressStateEnum;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * The evaluated state of a location for a plan (one row per plan and location). The plan and
 * location are held as plain identifiers (foreign keys in the database) so syncing a row never
 * loads the Plan or Location graphs.
 */
@FieldNameConstants
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityProgress {

  @Id
  @GeneratedValue
  private UUID identifier;

  private UUID planIdentifier;

  private UUID locationIdentifier;

  @Enumerated(EnumType.STRING)
  private ActivityProgressStateEnum state;

  private Integer total;

  private Integer notVisited;

  private Integer inProgress;

  private Integer complete;

  private Long serverVersion;

  private LocalDateTime modifiedDatetime;
}
