package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.persistence.primarykey.AssignedStructureCountsId;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@IdClass(AssignedStructureCountsId.class)
public class AssignedStructureCounts {

  @Id
  private UUID planIdentifier;
  @Id
  private UUID parentLocationIdentifier;

  private String parentLocationName;

  private String parentGeographicLevelName;

  @Id
  private String locationIdentifier;

  private String locationName;

  private String geographicLevelName;

  private long structureCount;
}
