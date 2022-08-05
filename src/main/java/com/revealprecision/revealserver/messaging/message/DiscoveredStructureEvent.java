package com.revealprecision.revealserver.messaging.message;


import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DiscoveredStructureEvent extends Message {
  private UUID locationIdentifier;
}
