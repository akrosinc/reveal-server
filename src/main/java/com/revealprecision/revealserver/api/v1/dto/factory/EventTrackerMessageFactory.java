package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.facade.models.EventFacade;
import com.revealprecision.revealserver.api.v1.facade.models.Obs;
import com.revealprecision.revealserver.messaging.message.EventTrackerMessage;
import com.revealprecision.revealserver.persistence.domain.Event;
import com.revealprecision.revealserver.persistence.domain.Plan;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public  class EventTrackerMessageFactory {
  public static EventTrackerMessage getEntity(Event savedEvent, EventFacade eventFacade,
      Plan plan, String dateString,
      String supervisorName, String cdd, UUID baseEntityIdentifier, String aggregationKey) {
    return EventTrackerMessage.builder()
        .aggregationKey(aggregationKey)
        .eventType(eventFacade.getEventType())
        .deviceUser(cdd)
        .locationIdentifier(baseEntityIdentifier)
        .observations(unpackObservations(eventFacade.getObs()))
        .planIdentifier(plan.getIdentifier())
        .operationDatetime(dateString)
        .supervisor(supervisorName)
        .contributingEvents(List.of(savedEvent.getIdentifier()))
        .build();
  }

  private static Map<String, List<Object>> unpackObservations(List<Obs> obs) {

    return obs.stream()
        .collect(Collectors.toMap(Obs::getFormSubmissionField, Obs::getValues, (a, b) -> b));

  }
}
