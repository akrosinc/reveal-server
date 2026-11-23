package com.revealprecision.revealserver.persistence.projection;

public interface LocationEntityTagCountProjection {

  String getTag();

  // value set for the location itself
  Double getOwnCount();

  // value of the location plus the values set for its descendants
  Double getTotalCount();
}
