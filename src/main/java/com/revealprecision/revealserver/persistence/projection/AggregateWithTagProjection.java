package com.revealprecision.revealserver.persistence.projection;

import com.revealprecision.revealserver.persistence.domain.EntityTag;


public interface AggregateWithTagProjection {
    EntityTag getTag();

    Double getSum();

    Double getAvg();

    Double getMedian();

    Double getMin();

    Double getMax();

    String getLocationIdentifier();

    String getEventType();

}