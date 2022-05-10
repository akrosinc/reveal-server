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
public class PersonMetadataEvent extends Message {

  private UUID identifier;
  private List<MetaDataEvent> metaDataEvents;
  private UUID entityId;
  private UUID ancestorNode;
  private UUID hierarchyIdentifier;
  private List<UUID> locationIdList;
}
