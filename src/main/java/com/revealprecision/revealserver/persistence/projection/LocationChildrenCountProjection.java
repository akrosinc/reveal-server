package com.revealprecision.revealserver.persistence.projection;

public interface LocationChildrenCountProjection {

  String getParentIdentifier();
  Long getChildrenCount();
}
