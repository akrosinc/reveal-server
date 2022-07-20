package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.messaging.message.FormFieldEvent;
import com.revealprecision.revealserver.persistence.domain.FormField;

public class FormFieldEventFactory {
  public static FormFieldEvent getFormFieldEvent(FormField formField) {
    return FormFieldEvent.builder()
        .dataType(formField.getDataType())
        .display(formField.getDisplay())
        .dataType(formField.getDataType())
        .identifier(formField.getIdentifier())
        .formTitle(formField.getFormTitle())
        .name(formField.getName())
        .build();
  }
}
