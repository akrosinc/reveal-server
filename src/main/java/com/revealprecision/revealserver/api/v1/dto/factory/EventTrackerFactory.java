package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.messaging.message.EventTrackerMessage;
import com.revealprecision.revealserver.persistence.domain.EventTracker;

public  class EventTrackerFactory {
  public static EventTracker getEntity(EventTrackerMessage eventTrackerMessage){
    return EventTracker.builder()
        .aggregationKey(eventTrackerMessage.getAggregationKey())
        .eventType(eventTrackerMessage.getEventType())
        .contributingEvents(eventTrackerMessage.getContributingEvents())
        .deviceUser(eventTrackerMessage.getDeviceUser())
        .supervisor(eventTrackerMessage.getSupervisor())
        .locationIdentifier(eventTrackerMessage.getLocationIdentifier())
        .observations(eventTrackerMessage.getObservations())
        .operationDatetime(eventTrackerMessage.getOperationDatetime())
        .planIdentifier(eventTrackerMessage.getPlanIdentifier())
        .taskIdentifier(eventTrackerMessage.getTaskIdentifier())
        .build();
  }

  public static EventTracker updateEntity(EventTracker eventTracker, EventTrackerMessage eventTrackerMessage){

    eventTracker.contributingEvents().add(eventTrackerMessage.getIdentifier());

    return eventTracker
        .aggregationKey(eventTrackerMessage.getAggregationKey())
        .eventType(eventTrackerMessage.getEventType())
        .contributingEvents(eventTrackerMessage.getContributingEvents())
        .deviceUser(eventTrackerMessage.getDeviceUser())
        .supervisor(eventTrackerMessage.getSupervisor())
        .locationIdentifier(eventTrackerMessage.getLocationIdentifier())
        .observations(eventTrackerMessage.getObservations())
        .operationDatetime(eventTrackerMessage.getOperationDatetime())
        .planIdentifier(eventTrackerMessage.getPlanIdentifier())
        .taskIdentifier(eventTrackerMessage.getTaskIdentifier());

  }
}
