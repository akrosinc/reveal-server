package com.revealprecision.revealserver.persistence.projection;

public interface EventMaxVersionProjection {

  int getVersion();

  String getIdentifier();

  String getEventType();

}
