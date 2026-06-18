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

@Setter
@Getter
@Entity
@FieldNameConstants
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataExtractQuery {

  @Id
  @GeneratedValue
  private UUID id;

  private UUID planIdentifier;

  private String queryLabel;

  private String query;

  private boolean custom;
}
