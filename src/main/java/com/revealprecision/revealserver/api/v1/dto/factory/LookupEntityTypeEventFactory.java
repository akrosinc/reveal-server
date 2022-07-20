package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.messaging.message.LookupEntityTypeEvent;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import java.util.UUID;

public class LookupEntityTypeEventFactory {
  public static LookupEntityTypeEvent getLookupEntityTypeEvent(LookupEntityType lookupEntityType) {
    return LookupEntityTypeEvent.builder()
        .tableName(lookupEntityType.getTableName())
        .code(lookupEntityType.getCode())
        .identifier(lookupEntityType.getIdentifier())
        .build();
  }
}
