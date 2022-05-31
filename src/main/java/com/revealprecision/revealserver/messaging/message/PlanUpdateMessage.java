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
public class PlanUpdateMessage extends Message {

  private UUID planIdentifier;

  private PlanUpdateType planUpdateType;

  private String ownerId;

}
