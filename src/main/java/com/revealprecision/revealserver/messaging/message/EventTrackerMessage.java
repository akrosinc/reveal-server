package com.revealprecision.revealserver.messaging.message;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class EventTrackerMessage extends Message {

  private UUID identifier;
  private String aggregationKey;
  private UUID locationIdentifier;
  private UUID planIdentifier;
  private String eventType;
  private UUID taskIdentifier;
  private Map<String, List<Object>> observations;
  private String supervisor;
  private String deviceUser;
  private String operationDatetime;
  private List<UUID> contributingEvents;

}
