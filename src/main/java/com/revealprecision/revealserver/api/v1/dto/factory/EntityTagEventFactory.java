package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.persistence.domain.EntityTag;

public class EntityTagEventFactory {

  public static EntityTagEvent getEntityTagEvent(EntityTag entityTag) {
    return EntityTagEvent
        .builder()
        .aggregationMethod(entityTag.getAggregationMethod())
        .definition(entityTag.getDefinition())
        .tag(entityTag.getTag())
        .valueType(entityTag.getValueType())
        .identifier(entityTag.getIdentifier())
        .isAggregate(entityTag.isAggregate())
        .build();
  }


}
