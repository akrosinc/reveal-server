package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.FormFieldRequest;
import com.revealprecision.revealserver.persistence.domain.FormField;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FormFieldFactory {

  public static FormField toEntity(FormFieldRequest formFieldRequest) {

    return FormField.builder()
        .display(formFieldRequest.getDisplay())
        .name(formFieldRequest.getName())
        .dataType(formFieldRequest.getDataType())
        .formTitle(formFieldRequest.getFormTitle())
        .build();
  }
}
