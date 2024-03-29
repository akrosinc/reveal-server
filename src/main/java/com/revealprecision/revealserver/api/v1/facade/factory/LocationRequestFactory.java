package com.revealprecision.revealserver.api.v1.facade.factory;

import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.api.v1.facade.models.CreateLocationRequest;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.persistence.domain.LocationProperty;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationRequestFactory {

  public static LocationRequest fromPhysicalLocationRequest(
      CreateLocationRequest physicalLocation) {
    LocationProperty locationPropertyRequest = LocationProperty.builder()
        .name(physicalLocation.getProperties().getName()).externalId(
            UUID.fromString(physicalLocation.getId()))
        .geographicLevel(physicalLocation.getProperties().getGeographicLevel())
        .status(physicalLocation.getProperties().getStatus()).build();

    if (physicalLocation.getProperties().getSurveyLocationType() != null) {
      locationPropertyRequest.setSurveyLocationType(
          physicalLocation.getProperties().getSurveyLocationType());
    }

    if (physicalLocation.getProperties().getStructureNumber() != null) {
      locationPropertyRequest.setStructureNumber(physicalLocation.getProperties().getStructureNumber());
    }

    if (physicalLocation.getProperties().getType() != null) {
      if (physicalLocation.getProperties().getType().equals(LocationConstants.BODY_OF_WATER)) {
        locationPropertyRequest.setSurveyLocationType(LocationConstants.WATERBODY);
      }
    }

    LocationRequest locationRequest = LocationRequest.builder()
        .geometry(physicalLocation.getGeometry())
        .type(physicalLocation.getType())
        .properties(locationPropertyRequest)
        .build();
    return locationRequest;
  }

  public static Map<LocationRequest, UUID> fromPhysicalLocationRequests(
      List<CreateLocationRequest> physicalLocationRequest) {
    return physicalLocationRequest.stream().collect(Collectors.toMap(
        createLocationRequest -> LocationRequestFactory.fromPhysicalLocationRequest(
            createLocationRequest), createLocationRequest -> UUID.fromString(
            createLocationRequest.getProperties().getParentId())));
  }
}
