package com.revealprecision.revealserver.persistence.domain.aggregation;


import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
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
public class EventAggregationStringCount {

  @Id
  @GeneratedValue
  private int id;

  private String name;

  private UUID ancestor;

  private UUID planIdentifier;

  private String eventType;

  private String fieldCode;

  private String val;

}
