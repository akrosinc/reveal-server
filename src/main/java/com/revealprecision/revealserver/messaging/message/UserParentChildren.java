package com.revealprecision.revealserver.messaging.message;

import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class UserParentChildren extends Message {

  private UUID planIdentifier;
  private UserLevel parent;
  private Set<UserLevel> children;
}
