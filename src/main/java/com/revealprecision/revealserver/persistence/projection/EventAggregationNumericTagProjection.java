package com.revealprecision.revealserver.persistence.projection;

public interface EventAggregationNumericTagProjection {

  String getEventTagSum();

  String getEventTagAverage();

  String getEventTagMedian();
}
