package com.revealprecision.revealserver.messaging.message;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
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

  private boolean addToMetadata;

}
