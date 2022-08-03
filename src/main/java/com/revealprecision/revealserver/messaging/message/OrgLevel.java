package com.revealprecision.revealserver.messaging.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class OrgLevel extends Message {

  private String orgId;
  private String name;
  private Integer level;
}
