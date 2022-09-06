package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.persistence.primarykey.LiteStructureCountId;
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
@IdClass(LiteStructureCountId.class)
public class LiteStructureCount {

  @Id
  private UUID locationHierarchyIdentifier;
  @Id
  private UUID parentLocationIdentifier;

  private String parentLocationName;

  private int structureCounts;
}
