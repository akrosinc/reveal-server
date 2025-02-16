package com.revealprecision.revealserver.persistence.domain;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
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
  private UUID id;

  private String compoundId;

  private String householdId;

  private String individualId;

  private UUID structureId;

  private String name;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private Fields fields;

  private String floatingLocationId;

  private String floatingLocationName;

  private String floatingLocationGeographicLevel;

  private String floatingHouseLocationName;

  private long serverVersion;

}
