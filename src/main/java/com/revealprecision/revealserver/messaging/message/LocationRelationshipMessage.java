package com.revealprecision.revealserver.messaging.message;

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
public class LocationRelationshipMessage extends Message {

  private UUID locationIdentifier;

  private UUID parentLocationIdentifier;

  private String locationName;

  private String geoName;

  private List<UUID> ancestry;

}
