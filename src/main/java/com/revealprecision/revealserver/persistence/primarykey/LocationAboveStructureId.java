package com.revealprecision.revealserver.persistence.primarykey;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class LocationAboveStructureId implements Serializable {


  private UUID locationHierarchyIdentifier;

  private UUID planIdentifier;

  private UUID parentLocationIdentifier;

  private UUID locationAboveStructureIdentifier;

}
