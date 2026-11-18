package com.revealprecision.revealserver.api.v1.facade.models;

import com.revealprecision.revealserver.enums.ActivityProgressStateEnum;
import com.revealprecision.revealserver.persistence.domain.ActivityProgress;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Wire format for ActivityProgress sync (push and pull). The key is (planIdentifier,
 * locationIdentifier); {@code identifier} is informational only.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityProgressDto {

  private UUID identifier;
  private UUID planIdentifier;
  private UUID locationIdentifier;
  private ActivityProgressStateEnum state;
  private Integer total;
  private Integer notVisited;
  private Integer inProgress;
  private Integer complete;
  private Long serverVersion;

  public static ActivityProgressDto from(ActivityProgress entity) {
    return ActivityProgressDto.builder()
        .identifier(entity.getIdentifier())
        .planIdentifier(entity.getPlanIdentifier())
        .locationIdentifier(entity.getLocationIdentifier())
        .state(entity.getState())
        .total(entity.getTotal())
        .notVisited(entity.getNotVisited())
        .inProgress(entity.getInProgress())
        .complete(entity.getComplete())
        .serverVersion(entity.getServerVersion())
        .build();
  }
}
