package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagEvent;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagValueEvent;
import com.revealprecision.revealserver.messaging.message.FormFieldEvent;
import com.revealprecision.revealserver.persistence.domain.FormField;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.util.EntityTagEventUtil;

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
      Object value) {
    return FormDataEntityTagValueEvent
        .builder()
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
}
