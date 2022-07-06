package com.revealprecision.revealserver.messaging.message;

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
public class LocationMetadataUnpackedEvent extends Message {

  private UUID identifier;
  private UUID entityId;
  private UUID ancestorNode;
  private UUID hierarchyIdentifier;
  private MetaDataEvent metaDataEvent;
  private UUID plan;
  private String planTargetType;
  private String entityGeoLevel;
  private Long structureCounts;
}
