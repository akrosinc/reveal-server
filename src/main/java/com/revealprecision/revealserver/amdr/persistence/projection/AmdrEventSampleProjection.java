package com.revealprecision.revealserver.amdr.persistence.projection;

import java.time.LocalDateTime;

public interface AmdrEventSampleProjection {

  String getLocationName();

  String getBarcode();

  LocalDateTime getCaptureDatetime();

}
