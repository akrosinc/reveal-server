package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@FieldNameConstants
@Audited
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@SQLDelete(sql = "UPDATE form SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class Form extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private boolean template;

  @Type(type = "jsonb")
  @Column(nullable = false)
  private JsonNode payload;

  @OneToMany(mappedBy = "form")
  private Set<Action> actions;

  @OneToMany(mappedBy = "form")
  private Set<FormData> formData;
}
