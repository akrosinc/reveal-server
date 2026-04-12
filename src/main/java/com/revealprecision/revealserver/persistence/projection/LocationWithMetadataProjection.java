package com.revealprecision.revealserver.persistence.projection;

import com.revealprecision.revealserver.persistence.domain.Geometry;
import java.time.LocalDate;

public interface LocationWithMetadataProjection {
    String getId();
    String getName();
    String getType();
    String getGeometry();
    String getGeographicLevel();
    String getTag();
    Double getSum();
    Double getAvg();
    Double getMedian();
    Double getMin();
    Double getMax();
    Long getCount();
    LocalDate getDataCaptureDate();
    Integer getYear();
}