package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.LocationPropertyResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.enums.SummaryEnum;
import com.revealprecision.revealserver.persistence.domain.Location;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationResponseFactory {

  public static LocationResponse fromEntity(Location location) {
    return LocationResponse.builder().identifier(location.getIdentifier())
        .type(location.getType()).geometry(location.getGeometry()).properties(
            LocationPropertyResponse.builder().name(location.getName()).status(location.getStatus())
                .externalId(location.getExternalId())
                .geographicLevel(location.getGeographicLevel().getName()).build()).build();
  }

  public static LocationResponse fromEntitySummary(Location location) {
    return LocationResponse.builder().identifier(location.getIdentifier())
        .type(location.getType()).properties(
            LocationPropertyResponse.builder().name(location.getName()).status(location.getStatus())
                .externalId(location.getExternalId())
                .geographicLevel(location.getGeographicLevel().getName()).build()).build();
  }

  public static Page<LocationResponse> fromEntityPage(Page<Location> locations, Pageable pageable,
      SummaryEnum summary) {
    var locationsResponseContent = locations.getContent().stream()
        .map(summary.equals(SummaryEnum.TRUE) ? LocationResponseFactory::fromEntitySummary
            : LocationResponseFactory::fromEntity).collect(
            Collectors.toList());
    return new PageImpl<>(locationsResponseContent, pageable, locations.getTotalElements());
  }
}
