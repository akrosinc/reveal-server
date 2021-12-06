package com.revealprecision.revealserver.api.dto.factory;

import com.revealprecision.revealserver.api.dto.request.GeographicLevelRequest;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GeographicLevelRequestFactory {

  public static GeographicLevel toEntity(GeographicLevelRequest geographicLevelRequest) {
    return GeographicLevel.builder()
        .name(geographicLevelRequest.getName())
        .title(geographicLevelRequest.getTitle()).build();
  }
}
