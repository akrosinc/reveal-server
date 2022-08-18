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
public class LocationFormDataSumAggregateEvent extends Message {

  private Double sum;
  private Long counter;
  private Double average;
  private UUID entityTagIdentifier;

}
