package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.model.ActionTemplateConfig;
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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants
@Entity
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE action_template SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class ActionTemplate extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  private String name;

  @Type(type = "jsonb")
  @Column(name = "action_template_config", columnDefinition = "jsonb")
  private ActionTemplateConfig actionTemplateConfig;

}
