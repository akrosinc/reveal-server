package com.revealprecision.revealserver.persistence.projection;

import java.math.BigDecimal;

public interface LocationCoordinatesProjection {

  BigDecimal getLongitude();

  BigDecimal getLatitude();

  String getName();

  String getIdentifier();
}
