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
public class ImportAggregationNumeric  {

  @Id
  @GeneratedValue
  private int id;

  private String name;

  private String hierarchyIdentifier;

  private String ancestor;

  private String planIdentifier;

  private String eventType;

  private String fieldCode;

  private Double val;


}
