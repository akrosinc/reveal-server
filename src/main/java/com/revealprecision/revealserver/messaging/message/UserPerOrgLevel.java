package com.revealprecision.revealserver.messaging.message;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class UserPerOrgLevel extends Message {

  private UUID planIdentifier;
  private LocalDateTime captureDateTime;
  private UserLevel userLevel;
  private Map<String, Object> fields;
}
