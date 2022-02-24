package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.LookupTaskStatusResponse;
import com.revealprecision.revealserver.persistence.domain.LookupTaskStatus;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LookupTaskStatusResponseFactory {

  public static LookupTaskStatusResponse fromEntity(LookupTaskStatus lookupTaskStatus) {
    return LookupTaskStatusResponse.builder()
        .identifier(lookupTaskStatus.getIdentifier())
        .name(lookupTaskStatus.getName())
        .code(lookupTaskStatus.getCode())
        .build();
  }

  public static List<LookupTaskStatusResponse> fromEntityList(
      List<LookupTaskStatus> lookupTaskStatuses) {
    return lookupTaskStatuses.stream()
        .map(LookupTaskStatusResponseFactory::fromEntity)
        .collect(Collectors.toList());
  }
}
