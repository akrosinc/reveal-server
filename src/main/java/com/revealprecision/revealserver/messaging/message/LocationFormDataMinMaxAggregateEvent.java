package com.revealprecision.revealserver.messaging.message;

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
public class LocationFormDataMinMaxAggregateEvent extends Message {

  private Double min = Double.MAX_VALUE;
  private Double max = Double.MIN_VALUE;
  private UUID entityTagIdentifier;

}
