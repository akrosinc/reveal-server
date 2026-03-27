package com.revealprecision.revealserver.persistence.projection;

import java.util.List;
import java.util.UUID;

public interface LocationRelationshipAncestryIdentifierProjection {

  UUID getIdentifier();
  String getLocationName();
  UUID getLocationIdentifier();
  UUID getParentIdentifier();
  String getGeographicLevelName();
  List<UUID> getAncestry();
}
