package com.revealprecision.revealserver.persistence.projection;

public interface ImportAggregateStringCountProjection {

  String getName();

  String getHierarchyIdentifier();

  String getLocationIdentifier();

  String getPlanIdentifier();

  String getEventType();

  String getFieldCode();

  String getFieldVal();

  Double getCount();
}
