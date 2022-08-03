package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.LocationMetadataImport;

import com.revealprecision.revealserver.messaging.message.LocationMetadataEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationMetadataImportFactory {

  public static LocationMetadataImport fromEntity(LocationMetadataEvent locationMetadata) {
    return LocationMetadataImport.builder().identifier(locationMetadata.getIdentifier())
        .locationIdentifier(locationMetadata.getEntityId())
        .locationName(locationMetadata.getLocationName())
        .entityValue(locationMetadata.getMetaDataEvents())
        .build();
  }

}
