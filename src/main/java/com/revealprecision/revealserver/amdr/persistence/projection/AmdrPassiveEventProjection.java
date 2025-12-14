package com.revealprecision.revealserver.amdr.persistence.projection;

import java.time.LocalDateTime;

public interface AmdrPassiveEventProjection {

  String getLocationName();

  String getLocationIdentifier();

  String getBarcode();

  LocalDateTime getCaptureDatetime();

}
