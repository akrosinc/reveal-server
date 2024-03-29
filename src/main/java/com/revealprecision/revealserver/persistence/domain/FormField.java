package com.revealprecision.revealserver.persistence.domain;

import java.util.Set;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@FieldNameConstants
@Entity
@Audited
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "UPDATE condition SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "formTitle"})
})
public class FormField extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  private String name;

  private String display;

  private String dataType;

  @ManyToMany(fetch = FetchType.LAZY)
  private Set<EntityTag> entityTags;

  private String formTitle;
}
