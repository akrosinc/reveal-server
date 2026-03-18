package com.revealprecision.revealserver.persistence.projection;

import java.util.UUID;


public interface UserIdInstanceNameProjection {
  UUID getUserIdentifier();
  String getInstanceName();
}
