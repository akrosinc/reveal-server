package com.revealprecision.revealserver.api.v1.facade.factory;

import com.revealprecision.revealserver.api.v1.facade.models.LocationPropertyFacade;
import com.revealprecision.revealserver.api.v1.facade.models.PhysicalLocation;
import com.revealprecision.revealserver.enums.LocationStatus;
import com.revealprecision.revealserver.persistence.projection.LocationWithParentProjection;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PhysicalLocationResponseFactory {

  public static PhysicalLocation fromLocationWithParentProjection(LocationWithParentProjection locationWithParentProjection){

    LocationPropertyFacade locationPropertyFacade = LocationPropertyFacade.builder()
        .name(locationWithParentProjection.getName())
        .geographicLevel(locationWithParentProjection.getGeographicLevelName())
        .parentId(locationWithParentProjection.getParentIdentifier())
        .status(LocationStatus.valueOf(locationWithParentProjection.getStatus()))
        .build();

    return PhysicalLocation.builder()
        .id(locationWithParentProjection.getIdentifier())
        .type(locationWithParentProjection.getType())
        .properties(locationPropertyFacade)
        .serverVersion(locationWithParentProjection.getServerVersion())
        .build();
  }


}
