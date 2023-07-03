package com.revealprecision.revealserver.persistence.domain;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AggregationStaging {

  @Id
  @GeneratedValue
  private Integer id;

  private UUID locationIdentifier;

  private UUID hierarchyIdentifier;

  private String nodeOrder;

}
