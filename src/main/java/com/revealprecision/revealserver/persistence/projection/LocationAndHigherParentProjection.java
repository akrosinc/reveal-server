package com.revealprecision.revealserver.persistence.projection;

public interface LocationAndHigherParentProjection {
  String getLocationIdentifier();
  String getLocationName();
  String getLocationGeographicLevelName();
  String getHigherLocationParentIdentifier();
  String getHigherLocationParentName();
  String getHigherLocationParentGeographicLevelName();
}
