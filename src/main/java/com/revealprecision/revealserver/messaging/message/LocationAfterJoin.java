package com.revealprecision.revealserver.messaging.message;


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
public class LocationAfterJoin extends Message {

  private UUID locationIdentifier;

  private UUID parentLocationIdentifier;

  private String geoName;

  private boolean isAssigned;

  private String planIdentifier;

}
