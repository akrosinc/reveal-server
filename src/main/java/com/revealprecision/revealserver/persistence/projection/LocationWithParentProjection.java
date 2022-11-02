package com.revealprecision.revealserver.persistence.projection;

public interface LocationWithParentProjection {

  String getIdentifier();

  String getName();

  String getGeometry();

  String getType();

  String getStatus();

  String getExternalId();

  String getGeographicLevelName();

  String getEntityStatus();

  Long getServerVersion();

  String getHashValue();

  String getParentIdentifier();

  String getLocationProperty();

}
