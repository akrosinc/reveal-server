package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.LocationMetadataImport;

import com.revealprecision.revealserver.persistence.domain.metadata.LocationMetadata;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationMetadataImportFactory {

  public static LocationMetadataImport fromEntity(LocationMetadata locationMetadata) {
    return LocationMetadataImport.builder().identifier(locationMetadata.getIdentifier())
        .locationIdentifier(locationMetadata.getLocation().getIdentifier())
        .locationName(locationMetadata.getLocation().getName())
        .entityValue(locationMetadata.getEntityValue())
        .build();
  }

}
