package com.revealprecision.revealserver.persistence.primarykey;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LocationCountsId implements Serializable {

  private UUID locationHierarchyIdentifier;

  private UUID parentLocationIdentifier;

  private UUID geographicLevelName;
}
