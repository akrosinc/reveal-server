package com.revealprecision.revealserver.persistence.projection;

public interface LocationBusinessStateCount {

  String getParentLocationIdentifier();

  String getPlanIdentifier();

  String getTaskBusinessStatus();

  Long getLocationCount();
}
