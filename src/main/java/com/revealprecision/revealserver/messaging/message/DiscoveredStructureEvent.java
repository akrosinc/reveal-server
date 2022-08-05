package com.revealprecision.revealserver.messaging.message;


import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveredStructureEvent extends Message {

  private UUID locationIdentifier;
}
