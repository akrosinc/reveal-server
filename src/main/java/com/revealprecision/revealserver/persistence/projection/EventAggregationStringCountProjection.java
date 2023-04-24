package com.revealprecision.revealserver.persistence.projection;

public interface EventAggregationStringCountProjection {

  String getName();

  String getLocationIdentifier();

  String getPlanIdentifier();

  String getEventType();

  String getFieldCode();

  String getFieldVal();

  Double getCount();
}