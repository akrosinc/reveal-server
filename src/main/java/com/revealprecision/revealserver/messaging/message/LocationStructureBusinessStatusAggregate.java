package com.revealprecision.revealserver.messaging.message;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class LocationStructureBusinessStatusAggregate extends Message {

  private UUID entityId;

  private String businessStatus;

  private LocalDateTime updateTime;

  private List<UUID> ancestry;

  private String previousBusinessStatus;

  private String businessStatusKey;

  private Long structureCounts;

  private UUID hierarchyIdentifier;
  private UUID plan;
  private String planTargetType;
  private String entityGeoLevel;
  private UUID ancestorNode;

  private Long structureSum = 0L;

}
