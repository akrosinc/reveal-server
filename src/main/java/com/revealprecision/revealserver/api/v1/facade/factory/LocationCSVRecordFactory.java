package com.revealprecision.revealserver.api.v1.facade.factory;

import com.revealprecision.revealserver.api.v1.dto.csv.LocationCSVRecord;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import java.util.AbstractMap.SimpleEntry;
import java.util.Set;

public class LocationCSVRecordFactory {

  public static LocationCSVRecord getLocationCSVRecordFromLocationResponse(
      LocationResponse locationResponse,
      Set<SimpleEntry<String, Object>> metaObj) {

    return LocationCSVRecord.builder()
        .identifier(locationResponse.getIdentifier().toString())
        .geographicLevel(locationResponse.getProperties().getGeographicLevel())
        .name(locationResponse.getProperties().getName())
        .meta(metaObj)
        .build();
  }

}
