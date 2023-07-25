package com.revealprecision.revealserver.persistence.projection;

public interface AggregateProjection {

  String getName();

  String getHierarchyIdentifier();

  String getLocationIdentifier();

  String getPlanIdentifier();

  String getEventType();

  String getFieldCode();


}
