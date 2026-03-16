package com.revealprecision.revealserver.persistence.projection;

import java.util.UUID;

public interface GroupManagementProjection {
  UUID getIdentifier();
  String getName();
  String getOrganizationType();
}
