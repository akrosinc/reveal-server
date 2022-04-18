package com.revealprecision.revealserver.persistence.projection;

public interface LocationRelationshipProjection {

  String getIdentifier();
  String getLocationName();
  String getLocationIdentifier();
  String getGeometry();
  String getParentIdentifier();
  String getGeographicLevelName();
}
