package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.persistence.primarykey.LocationCountsId;
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
@IdClass(LocationCountsId.class)
public class LocationCounts {

  @Id
  private UUID locationHierarchyIdentifier;
  @Id
  private UUID parentLocationIdentifier;

  private String parentLocationName;

  private String parentGeographicLevelName;
  @Id
  private String geographicLevelName;

  private long locationCount;
}
