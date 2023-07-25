package com.revealprecision.revealserver.persistence.domain.aggregation;


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
public class ResourceAggregationNumeric {

  @Id
  @GeneratedValue
  private int id;

  private String locationName;

  private String hierarchyIdentifier;

  private String ancestor;

  private String planIdentifier;

  private String resourcePlanName;

  private String fieldCode;

  private Double val;


}
