package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.LookupEntityTypeResponse;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LookupEntityTagResponseFactory {

  public static LookupEntityTypeResponse fromEntity(LookupEntityType lookupEntityType) {
    return LookupEntityTypeResponse.builder().identifier(lookupEntityType.getIdentifier())
        .code(lookupEntityType.getCode()).tableName(lookupEntityType.getTableName()).build();
  }

}
