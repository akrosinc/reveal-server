package com.revealprecision.revealserver.model;

import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
public class AggregateNumeric implements Serializable {

  private String name;

  private String locationIdentifier;

  private String planIdentifier;

  private String eventType;

  private String fieldType;

  private String fieldCode;

  private Double sum;

  private Double avg;

  private Double median;

  private Double min;

  private Double max;

}
