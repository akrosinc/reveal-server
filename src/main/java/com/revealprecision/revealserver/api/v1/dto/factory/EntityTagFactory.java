package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.constants.EntityTagDataTypes;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityTagFactory {

  public static EntityTag toEntity(EntityTagRequest entityTagRequest) {

    return EntityTag.builder()
        .tag(entityTagRequest.getTag())
        .definition(entityTagRequest.getDefinition())
        .valueType(entityTagRequest.getValueType().equals("number")? EntityTagDataTypes.DOUBLE :entityTagRequest.getValueType())
        .aggregationMethod(entityTagRequest.getAggregationMethod())
        .isAggregate(entityTagRequest.isAggregate())
        .build();
  }
}
