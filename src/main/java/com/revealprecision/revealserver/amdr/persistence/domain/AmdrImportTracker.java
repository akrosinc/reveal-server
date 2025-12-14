package com.revealprecision.revealserver.amdr.persistence.domain;

import java.util.UUID;
import javax.persistence.Column;
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
import org.hibernate.envers.Audited;

@FieldNameConstants
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(schema = "amdr")
@Audited
public class AmdrImportTracker  {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(nullable = false)
  private UUID amdrImportId;

  private String state;

}
