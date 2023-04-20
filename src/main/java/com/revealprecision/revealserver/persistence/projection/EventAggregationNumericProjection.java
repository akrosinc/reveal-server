package com.revealprecision.revealserver.persistence.projection;

public interface EventAggregationNumericProjection {

  String getName();

  String getLocationIdentifier();

  String getPlanIdentifier();

  String getEventType();

  String getFieldCode();

  Double getSum();

  Double getAvg();

  Double getMedian();

}
