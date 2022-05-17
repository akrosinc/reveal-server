package com.revealprecision.revealserver.persistence.projection;

public interface LocationRelationshipProjection {

  String getIdentifier();
  String getLocationName();
  String getLocationIdentifier();
  String getParentIdentifier();
  String getGeographicLevelName();
  long getChildrenNumber();
}
