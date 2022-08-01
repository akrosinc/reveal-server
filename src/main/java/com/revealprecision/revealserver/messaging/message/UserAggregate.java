package com.revealprecision.revealserver.messaging.message;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class UserAggregate extends Message {

  private UUID planIdentifier;
  private UserLevel userLevel;
  private LocalDateTime minDateTime;
  private LocalDateTime maxDateTime;
  private Long daysWorked;
  private Long daysWorkedRepeat;
  private Long minutesWorked;
  private Long minutesWorkedRepeat;
  private Long startTime;
  private Long endTime;
}
