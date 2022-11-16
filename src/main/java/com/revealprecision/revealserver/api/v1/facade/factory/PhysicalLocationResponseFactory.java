package com.revealprecision.revealserver.api.v1.facade.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.facade.models.LocationPropertyFacade;
import com.revealprecision.revealserver.api.v1.facade.models.PhysicalLocation;
import com.revealprecision.revealserver.enums.LocationStatus;
import com.revealprecision.revealserver.persistence.domain.Geometry;
import com.revealprecision.revealserver.persistence.domain.LocationProperty;
import com.revealprecision.revealserver.persistence.projection.LocationWithParentProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PhysicalLocationResponseFactory {

  private final ObjectMapper objectMapper;

  public PhysicalLocation fromLocationWithParentProjection(
      LocationWithParentProjection locationWithParentProjection) {

    LocationPropertyFacade locationPropertyFacade = LocationPropertyFacade.builder()
        .name(locationWithParentProjection.getName())
        .geographicLevel(locationWithParentProjection.getGeographicLevelName())
        .parentId(locationWithParentProjection.getParentIdentifier())
        .status(LocationStatus.valueOf(locationWithParentProjection.getStatus()))
        .build();

    Geometry geometry = null;
    LocationProperty locationProperty;
    try {
      geometry = objectMapper.readValue(locationWithParentProjection.getGeometry(),
          Geometry.class);

    } catch (JsonProcessingException e) {
      log.error("Cannot create geometry obj from string {}",
          locationWithParentProjection.getIdentifier(), e);
    }

    try {
      locationProperty = objectMapper.readValue(locationWithParentProjection.getLocationProperty(),
          LocationProperty.class);

      if (locationProperty != null && locationProperty.getSurveyLocationType() != null) {
        locationPropertyFacade.setSurveyLocationType(locationProperty.getSurveyLocationType());
      }
      if (locationProperty != null && locationProperty.getStructureNumber() != null) {
        locationPropertyFacade.setStructureNumber(locationProperty.getStructureNumber());
      }
    } catch (JsonProcessingException e) {
      log.error("Cannot create location properties obj from string {}",
          locationWithParentProjection.getIdentifier(), e);
    }
    return PhysicalLocation.builder()
        .id(locationWithParentProjection.getIdentifier())
        .type(locationWithParentProjection.getType())
        .properties(locationPropertyFacade)
        .serverVersion(locationWithParentProjection.getServerVersion())
        .geometry(geometry)
        .build();
  }


}
