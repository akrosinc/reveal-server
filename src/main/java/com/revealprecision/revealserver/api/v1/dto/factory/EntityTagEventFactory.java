package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import java.util.stream.Collectors;

public class EntityTagEventFactory {

  public static EntityTagEvent getEntityTagEvent(EntityTag entityTag) {
    return EntityTagEvent
        .builder()
        .aggregationMethod(entityTag.getAggregationMethod())
        .definition(entityTag.getDefinition())
        .formFields(
            entityTag.getFormFields() == null ? null
                : entityTag.getFormFields().stream().map(FormFieldEventFactory::getFormFieldEvent)
                    .collect(
                        Collectors.toSet()))
        .generated(entityTag.isGenerated())
        .generationFormula(entityTag.getGenerationFormula())
        .isResultLiteral(entityTag.isResultLiteral())
        .lookupEntityType(LookupEntityTypeEventFactory.getLookupEntityTypeEvent(
            entityTag.getLookupEntityType()))
        .referencedFields(entityTag.getReferencedFields())
        .resultExpression(entityTag.getResultExpression())
        .scope(entityTag.getScope())
        .tag(entityTag.getTag())
        .valueType(entityTag.getValueType())
        .identifier(entityTag.getIdentifier())
        .addToMetadata(entityTag.isAddToMetadata())
        .isAggregate(entityTag.isAggregate())
        .build();
  }


}
