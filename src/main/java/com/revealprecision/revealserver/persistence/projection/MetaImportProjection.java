package com.revealprecision.revealserver.persistence.projection;

import com.revealprecision.revealserver.enums.EntityStatus;

public interface MetaImportProjection {
  String getMessage();

  String getUsername();

  EntityStatus getEntityStatus();
}
