package com.revealprecision.revealserver.persistence.domain;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Type;
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
public class EntityTag extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  @Column(unique = true)
  private String tag;

  private String valueType;

  private String definition;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "lookup_entity_type_identifier", referencedColumnName = "identifier")
  private LookupEntityType lookupEntityType;

  @ManyToMany(cascade = CascadeType.ALL)
  @JoinTable(name = "form_field_entity_tag", joinColumns = @JoinColumn(name = "entity_tag_identifier"), inverseJoinColumns = @JoinColumn(name = "form_field_identifier"))
  private Set<FormField> formFields;

  private boolean generated;

  @Type(type = "list-array")
  private List<String> referencedFields;
  @Type(type = "list-array")
  private List<String> aggregationMethod;

  private String generationFormula;

  private String scope;

  private String resultExpression;

  private boolean isResultLiteral;

}
