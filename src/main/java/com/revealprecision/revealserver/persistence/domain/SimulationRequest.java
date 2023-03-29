package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.api.v1.dto.request.DataFilterRequest;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@FieldNameConstants
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class SimulationRequest {

  @Id
  @GeneratedValue(generator = "custom-generator")
  @GenericGenerator(name = "custom-generator", strategy = "com.revealprecision.revealserver.persistence.generator.CustomIdentifierGenerator")
  private UUID identifier;


  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private DataFilterRequest request;

}
