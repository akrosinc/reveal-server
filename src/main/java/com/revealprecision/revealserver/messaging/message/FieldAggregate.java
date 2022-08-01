package com.revealprecision.revealserver.messaging.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class FieldAggregate extends Message {

  private Long count;
  private Long Sum;
}
