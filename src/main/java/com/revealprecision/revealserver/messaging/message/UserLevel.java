package com.revealprecision.revealserver.messaging.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class UserLevel extends Message {

  private String userId;
  private String name;
  private Integer level;
  private String type;
  private String label;
}
