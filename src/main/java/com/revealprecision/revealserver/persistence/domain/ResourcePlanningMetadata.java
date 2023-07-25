package com.revealprecision.revealserver.persistence.domain;

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
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResourcePlanningMetadata {

  @Id
  @GeneratedValue
  private int identifier;

  private String locationIdentifier;

  private String tag;

  private Double value;

  private String resourcePlan;
}
