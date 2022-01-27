package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.LookupInterventionTypeResponse;
import com.revealprecision.revealserver.persistence.domain.LookupInterventionType;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LookupInterventionTypeResponseFactory {

  public static LookupInterventionTypeResponse fromEntity(LookupInterventionType interventionType) {
    return LookupInterventionTypeResponse.builder()
        .identifier(interventionType.getIdentifier())
        .name(interventionType.getName())
        .code(interventionType.getCode())
        .build();
  }

  public static List<LookupInterventionTypeResponse> fromEntityList(
      List<LookupInterventionType> interventionTypes) {
    return interventionTypes.stream()
        .map(LookupInterventionTypeResponseFactory::fromEntity)
        .collect(Collectors.toList());
  }
}
