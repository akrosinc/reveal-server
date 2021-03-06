package com.revealprecision.revealserver.messaging.message;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PersonMetadataEvent extends Message {

  private UUID identifier;

  private List<MetaDataEvent> metaDataEvents;

  private UUID entityId;

  private UUID ancestorNode;

  private UUID hierarchyIdentifier;

  private List<UUID> locationIdList;

  private UUID thisLocation;

  private UUID personIdentifier;

}
