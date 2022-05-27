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
public class LookupInterventionTypeEvent extends Message {

  private UUID identifier;

  private String name;

  private String code;
}
