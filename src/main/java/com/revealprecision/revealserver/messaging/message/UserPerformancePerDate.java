package com.revealprecision.revealserver.messaging.message;

import java.time.LocalTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class UserPerformancePerDate extends Message {

  private Long minutesWorked;
  private LocalTime minStartTime;
  private java.time.LocalTime maxEndTime;
  private Long startTimeLong;
  private Long endTimeLong;
  private Map<String, Object> fields;
  private Map<String, Map<String, FieldAggregate>> fieldAggregate;
}
