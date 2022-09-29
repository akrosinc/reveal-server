package com.revealprecision.revealserver.persistence.domain;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.List;
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
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Where(clause = "entity_status='ACTIVE'")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@FieldNameConstants
public class CampaignDrug {

  @Id
  @GeneratedValue
  private UUID identifier;

  private String name;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private List<Drug> drugs;

}
