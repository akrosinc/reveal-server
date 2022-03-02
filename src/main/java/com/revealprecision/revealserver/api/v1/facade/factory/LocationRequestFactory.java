package com.revealprecision.revealserver.api.v1.facade.factory;

import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.api.v1.facade.models.PhysicalLocation;
import com.revealprecision.revealserver.persistence.domain.LocationProperty;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationRequestFactory {


  public static LocationRequest fromPhysicalLocationRequest(PhysicalLocation physicalLocation) {
    LocationProperty locationPropertyRequest = LocationProperty.builder()
        .name(physicalLocation.getProperties().getName()).externalId(
            UUID.fromString(physicalLocation.getId()))
        .geographicLevel(physicalLocation.getProperties().getGeographicLevel())
        .status(physicalLocation.getProperties().getStatus().name()).build();

    LocationRequest locationRequest = LocationRequest.builder()
        .geometry(physicalLocation.getGeometry()).type(physicalLocation.getType()).properties(locationPropertyRequest)
        .build();
    return  locationRequest;
  }

  public static List<LocationRequest> fromPhysicalLocationRequests(List<PhysicalLocation> physicalLocationRequest){
   return  physicalLocationRequest.stream().map(LocationRequestFactory::fromPhysicalLocationRequest).collect(
        Collectors.toList());
  }
}
