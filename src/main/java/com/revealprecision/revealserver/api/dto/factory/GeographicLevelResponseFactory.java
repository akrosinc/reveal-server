package com.revealprecision.revealserver.api.dto.factory;

import com.revealprecision.revealserver.api.dto.response.GeographicLevelResponse;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GeographicLevelResponseFactory {

  public static GeographicLevelResponse fromEntity(GeographicLevel geographicLevel) {
    return GeographicLevelResponse.builder()
        .identifier(geographicLevel.getIdentifier())
        .name(geographicLevel.getName())
        .title(geographicLevel.getTitle())
        .build();
  }

  public static Page<GeographicLevelResponse> fromEntityPage(Page<GeographicLevel> geographicLevels,
      Pageable pageable) {
    var geographicLevelResponseContent = geographicLevels.getContent().stream()
        .map(GeographicLevelResponseFactory::fromEntity)
        .collect(Collectors.toList());
    return new PageImpl<>(geographicLevelResponseContent, pageable,
        geographicLevels.getTotalElements());
  }
}
