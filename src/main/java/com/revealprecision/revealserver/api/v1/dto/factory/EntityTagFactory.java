package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.FormField;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityTagFactory {

  public static EntityTag toEntity(EntityTagRequest entityTagRequest,
      LookupEntityType lookupEntityType, Set<FormField> formFields) {

    EntityTag entityTag = EntityTag.builder()
        .tag(entityTagRequest.getTag())
        .definition(entityTagRequest.getDefinition())
        .lookupEntityType(lookupEntityType)
        .valueType(entityTagRequest.getValueType())
        .aggregationMethod(entityTagRequest.getAggregationMethod())
        .generated(entityTagRequest.isGenerated())
        .referencedFields(entityTagRequest.getReferencedFields())
        .generationFormula(entityTagRequest.getGenerationFormula())
        .scope(entityTagRequest.getScope())
        .valueType(entityTagRequest.getValueType())
        .resultExpression(entityTagRequest.getResultExpression())
        .isResultLiteral(entityTagRequest.isResultLiteral())
        .addToMetadata(entityTagRequest.isAddToMetadata())
        .build();

    if (formFields != null) {
      entityTag.setFormFields(formFields);
    }

    entityTag.setEntityStatus(EntityStatus.ACTIVE);
    return entityTag;
  }
}
