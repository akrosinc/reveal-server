package com.revealprecision.revealserver.persistence.domain;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
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
public class LocationRelationships extends AbstractAuditableEntity {

  @Id
  private UUID locationHierarchyIdentifier;

  private UUID parentLocationIdentifier;

  private String parentLocationName;

  private String parentGeographicLevelName;

  private UUID locationIdentifier;

  private UUID geographicLevelName;

}
