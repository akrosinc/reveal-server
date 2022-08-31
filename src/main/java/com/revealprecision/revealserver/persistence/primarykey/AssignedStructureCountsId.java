package com.revealprecision.revealserver.persistence.primarykey;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class AssignedStructureCountsId implements Serializable {

  private UUID planIdentifier;

  private UUID parentLocationIdentifier;

  private String locationIdentifier;

}
