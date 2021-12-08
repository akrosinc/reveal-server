package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.persistence.domain.Location;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationResponseFactory {

  public static LocationResponse fromEntity(Location location) {
    return LocationResponse.builder().identifier(location.getIdentifier())
        .type(location.getType()).geometry(location.getGeometry()).build();
  }
}
