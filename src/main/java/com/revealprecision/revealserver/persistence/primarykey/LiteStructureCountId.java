package com.revealprecision.revealserver.persistence.primarykey;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class LiteStructureCountId implements Serializable {

  private UUID locationHierarchyIdentifier;

  private UUID parentLocationIdentifier;

}
