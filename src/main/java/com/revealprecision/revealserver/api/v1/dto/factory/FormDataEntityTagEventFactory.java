package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.messaging.message.FormDataEntityTagEvent;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagValueEvent;
import com.revealprecision.revealserver.persistence.domain.Event;
import com.revealprecision.revealserver.persistence.domain.Plan;
import java.util.List;
import java.util.UUID;

public class FormDataEntityTagEventFactory {

  // Proceed with caution here as new updates / removals to the object will prevent rewind of the streams application.
  // In the event of new data being introduced, ensure that null pointers are catered in the streams
  // application if the event comes through, and it does not have the new fields populated
  public static FormDataEntityTagEvent getEntity(Event savedEvent,
      List<FormDataEntityTagValueEvent> formDataEntityTagValueEvents, Plan plan,
      UUID baseEntityIdentifier, String dateString, String cdd, String supervisorName) {
    return FormDataEntityTagEvent.builder()
        .formDataEntityTagValueEvents(formDataEntityTagValueEvents)
        .eventId(savedEvent.getIdentifier().toString())
        .eventType(savedEvent.getEventType())
        .entityId(baseEntityIdentifier)
        .planIdentifier(savedEvent.getPlanIdentifier())
        .taskIdentifier(savedEvent.getTaskIdentifier())
        .locationHierarchyIdentifier(plan.getLocationHierarchy().getIdentifier())
        .date(dateString)
        .cddName(cdd)
        .user(savedEvent.getUser().getUsername())
        .supervisor(supervisorName)
        .build();
  }
}
