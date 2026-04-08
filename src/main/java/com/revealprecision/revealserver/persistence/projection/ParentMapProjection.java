package com.revealprecision.revealserver.persistence.projection;

public interface ParentMapProjection {

  String getLocationId();

  String getLocationName();

  String getParentId();

  String getParentName();

  int getNodeLevel();
}
