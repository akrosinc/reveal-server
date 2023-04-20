package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;

public class EntityTagRequestFactory {
  public static EntityTagRequest getCopy(EntityTagRequest entityTag) {

    return EntityTagRequest.builder()
        .tag(entityTag.getTag())
        .aggregationMethod(entityTag.getAggregationMethod())
        .scope(entityTag.getScope())
        .addToMetadata(entityTag.isAddToMetadata())
        .definition(entityTag.getDefinition())
        .valueType(entityTag.getValueType())
        .generationFormula(entityTag.getGenerationFormula())
        .generated(entityTag.isGenerated())
        .resultExpression(entityTag.getResultExpression())
        .formFieldNames(entityTag.getFormFieldNames())
        .isResultLiteral(entityTag.isResultLiteral())
        .entityType(
            entityTag.getEntityType())
        .isAggregate(entityTag.isAggregate())
        .build();
  }
}
