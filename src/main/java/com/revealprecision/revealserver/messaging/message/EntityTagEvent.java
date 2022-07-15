package com.revealprecision.revealserver.messaging.message;

import com.revealprecision.revealserver.persistence.domain.AbstractAuditableEntity;
import com.revealprecision.revealserver.persistence.domain.FormField;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
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
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EntityTagEvent extends Message {


  private UUID identifier;

  private String tag;

  private String valueType;

  private String definition;

  private LookupEntityTypeEvent lookupEntityType;

  private Set<FormFieldEvent> formFields;

  private boolean generated;

  private List<String> referencedFields;

  private List<String> aggregationMethod;

  private String generationFormula;

  private String scope;

  private String resultExpression;

  private boolean isResultLiteral;

}
