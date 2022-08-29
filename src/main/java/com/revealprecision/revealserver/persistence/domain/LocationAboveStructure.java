package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.persistence.primarykey.LocationAboveStructureId;
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
@IdClass(LocationAboveStructureId.class)
public class LocationAboveStructure {

  @Id
  private UUID locationHierarchyIdentifier;
  @Id
  private UUID planIdentifier;
  @Id
  private UUID parentLocationIdentifier;

  private String parentLocationName;

  private String parentGeographicLevelName;

  private String locationAboveStructureGeographicLevelName;
  private String locationAboveStructureName;
  @Id
  private UUID locationAboveStructureIdentifier;

  private boolean isVisited;
  private boolean isTreated;
  private boolean isVisitedEffectively;
}
