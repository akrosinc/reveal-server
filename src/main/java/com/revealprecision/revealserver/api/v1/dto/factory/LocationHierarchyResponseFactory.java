package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.LocationHierarchyResponse;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;

public class LocationHierarchyResponseFactory {

  public static LocationHierarchyResponse fromEntity(LocationHierarchy locationHierarchy) {
    return LocationHierarchyResponse.builder().identifier(locationHierarchy.getIdentifier())
        .nodeOrder(locationHierarchy.getNodeOrder()).build();
  }
}
