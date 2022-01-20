package com.revealprecision.revealserver.persistence.projection;

import com.revealprecision.revealserver.enums.EntityStatus;

public interface LocationBulkProjection {
  String getName();
  String getMessage();
  EntityStatus getEntityStatus();
}
