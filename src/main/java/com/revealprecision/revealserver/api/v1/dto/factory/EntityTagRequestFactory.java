package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;

public class EntityTagRequestFactory {
  public static EntityTagRequest getCopy(EntityTagRequest entityTag) {
    return EntityTagRequest.builder()
        .tag(entityTag.getTag())
        .aggregationMethod(entityTag.getAggregationMethod())
        .definition(entityTag.getDefinition())
        .valueType(entityTag.getValueType())
        .isAggregate(entityTag.isAggregate())
        .build();
  }
}
