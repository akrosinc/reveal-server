package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagEvent;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagValueEvent;
import com.revealprecision.revealserver.messaging.message.FormFieldEvent;
import com.revealprecision.revealserver.persistence.domain.FormField;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.util.EntityTagEventUtil;
import java.util.UUID;

public class FormDataEntityTagValueEventFactory {

  public static FormDataEntityTagValueEvent getEntity(Object value, FormField formField,
      EntityTagEvent entityTagEvent) {
    return FormDataEntityTagValueEvent.builder()
        .value(EntityTagEventUtil.getEntityTagEventValue(entityTagEvent,
            value))
        .entityTagEvent(entityTagEvent)
        .selectedformField(FormFieldEventFactory.getFormFieldEvent(formField))
        .build();
  }

  public static FormDataEntityTagValueEvent getEntity(Object value, FormFieldEvent formFieldEvent,
      EntityTagEvent entityTagEvent) {
    return FormDataEntityTagValueEvent.builder()
        .value(value)
        .entityTagEvent(entityTagEvent)
        .selectedformField(formFieldEvent)
        .build();
  }

  public static FormDataEntityTagValueEvent getEntity(FormDataEntityTagEvent eventMetadata,
      String dateForScopeDateFields, Location location, EntityTagEvent referencedTagEvent,
      Object value, UUID eventId) {
    return FormDataEntityTagValueEvent
        .builder()
        .eventId(eventId)
        .entityTagEvent(referencedTagEvent)
        .planIdentifier(eventMetadata.getPlanIdentifier())
        .locationHierarchyIdentifier(
            eventMetadata.getLocationHierarchyIdentifier())
        .geographicLevelName(eventMetadata.getGeographicalLevelName())
        .entityIdentifier(location.getIdentifier())
        .dateForScopeDate(dateForScopeDateFields)
        .cddName(eventMetadata.getCddName())
        .supervisor(eventMetadata.getSupervisor())
        .value(value)
        .build();
  }

  public static FormDataEntityTagValueEvent getEntity(UUID planIdentifier,
      UUID locationHierarchyIdentifier,
      String geoLevelName, String ccdName, String supervisorName,
      String dateForScopeDateFields, Location location, EntityTagEvent referencedTagEvent,
      Object value, UUID eventId) {
    return FormDataEntityTagValueEvent
        .builder()
        .eventId(eventId)
        .entityTagEvent(referencedTagEvent)
        .planIdentifier(planIdentifier)
        .locationHierarchyIdentifier(
            locationHierarchyIdentifier)
        .geographicLevelName(geoLevelName)
        .entityIdentifier(location.getIdentifier())
        .dateForScopeDate(dateForScopeDateFields)
        .cddName(ccdName)
        .supervisor(supervisorName)
        .value(value)
        .build();
  }
}
