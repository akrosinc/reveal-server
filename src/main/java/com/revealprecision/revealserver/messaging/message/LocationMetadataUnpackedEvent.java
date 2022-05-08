package com.revealprecision.revealserver.messaging.message;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Setter
@Getter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationMetadataUnpackedEvent extends Message {

  private UUID identifier;
  private UUID entityId;
  private UUID ancestorNode;
  private UUID hierarchyIdentifier;
  private MetaDataEvent metaDataEvent;
}
