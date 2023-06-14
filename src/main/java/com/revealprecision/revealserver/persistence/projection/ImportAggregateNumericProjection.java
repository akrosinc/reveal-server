package com.revealprecision.revealserver.persistence.projection;

public interface ImportAggregateNumericProjection {

  String getName();

  String getLocationIdentifier();

  String getPlanIdentifier();

  String getEventType();

  String getFieldCode();

  Double getSum();

  Double getAvg();

  Double getMedian();

}