package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityTagFactory {

  public static EntityTag toEntity(EntityTagRequest entityTagRequest,
      LookupEntityType lookupEntityType) {

    EntityTag entityTag = EntityTag.builder().tag(entityTagRequest.getTag())
        .definition(entityTagRequest.getDefinition()).lookupEntityType(lookupEntityType)
        .valueType(entityTagRequest.getValueType()).build();

    entityTag.setEntityStatus(EntityStatus.ACTIVE);
    return entityTag;
  }
}
