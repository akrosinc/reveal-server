package com.revealprecision.revealserver.persistence.projection;

public interface EntityTagWithGeoLevelAndEntityTypeProjection {

  String getTagName();

  String getTag();

  String getGeoName();

  String getEventType();

  String getGeoIdentifier();
}
