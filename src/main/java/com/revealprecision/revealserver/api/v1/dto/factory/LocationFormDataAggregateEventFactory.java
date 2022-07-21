package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.messaging.message.LocationFormDataAggregateEvent;

public class LocationFormDataAggregateEventFactory {
  public static LocationFormDataAggregateEvent getEntity(LocationFormDataAggregateEvent v, Long increment, Long averageCounter, String tagValue) {
    return LocationFormDataAggregateEvent.builder()
        .plan(v.getPlan())
        .entityId(v.getEntityId())
        .hierarchyIdentifier(v.getHierarchyIdentifier())
        .tag(v.getTag())
        .ancestorNode(v.getAncestorNode())
        .tagValue(tagValue)
        .averageCounter(averageCounter)
        .increment(increment)
        .supervisor(v.getSupervisor())
        .cddName(v.getCddName())
        .build();
  }
}
