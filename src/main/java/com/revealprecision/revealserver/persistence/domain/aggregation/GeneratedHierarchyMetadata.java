package com.revealprecision.revealserver.persistence.domain.aggregation;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
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

public class GeneratedHierarchyMetadata {

  @Id
  @GeneratedValue
  private int id;

  private String locationIdentifier;
  private String tag;
  private Double value;

  @ManyToOne
  private GeneratedHierarchy generatedHierarchy;

}
