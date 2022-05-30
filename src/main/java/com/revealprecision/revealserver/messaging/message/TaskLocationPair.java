package com.revealprecision.revealserver.messaging.message;

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
public class TaskLocationPair extends Message {

  private String name;

  private String id;

  private Long serverVersion;

}
