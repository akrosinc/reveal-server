package com.revealprecision.revealserver.api.v1.facade.factory;

import com.revealprecision.revealserver.api.v1.facade.models.EventFacade;
import com.revealprecision.revealserver.api.v1.facade.models.Obs;
import com.revealprecision.revealserver.persistence.domain.Event;
import java.util.List;
import java.util.Map;

public class EventFacadeFactory {

  public static EventFacade getEventFacade(Event event, List<Obs> obs, Map<String, String> details) {
    EventFacade eventFacade = EventFacade.builder().eventId(event.getIdentifier().toString())
        .baseEntityId(event.getBaseEntityIdentifier().toString())
        .eventType(event.getEventType())
        .details(
            details)
        .obs(obs)
        .providerId(event.getUser().getUsername())
        .teamId(event.getOrganization().getIdentifier().toString())
        .team(event.getOrganization().getName())
        .locationId(event.getLocationIdentifier().toString())
        .formSubmissionId(event.getAdditionalInformation().get("formSubmissionId").textValue())
        .entityType(event.getAdditionalInformation() == null ? null
            : event.getAdditionalInformation().get("entityType").textValue()).childLocationId(
            event.getAdditionalInformation() == null ? null
                : event.getAdditionalInformation().get("childLocationId") == null ? null
                    : event.getAdditionalInformation().get("childLocationId").textValue())
        .build();
    eventFacade.setType("Event");
    eventFacade.setDateCreated(event.getAdditionalInformation() == null ? null
        : event.getAdditionalInformation().get("dateCreated").textValue());
    return eventFacade;
  }
}
