package com.revealprecision.revealserver.persistence.domain;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
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
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(schema = "hdss")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)

public class HdssCompounds {

  @Id
  @GeneratedValue
  private UUID id;

  private String compoundId;

  private String householdId;

  private String individualId;

  private UUID structureId;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private Fields fields;

  private long serverVersion;

}
