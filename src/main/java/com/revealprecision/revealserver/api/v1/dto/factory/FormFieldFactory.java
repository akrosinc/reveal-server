package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.FormFieldRequest;
import com.revealprecision.revealserver.enums.ActionTitleEnum;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.persistence.domain.FormField;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FormFieldFactory {

  public static FormField toEntity(FormFieldRequest formFieldRequest) {

    FormField formField = FormField.builder()
        .display(formFieldRequest.getDisplay())
        .name(formFieldRequest.getName())
        .dataType(formFieldRequest.getDataType())
        .formTitle(formFieldRequest.getFormTitle())
        .build();

    formField.setEntityStatus(EntityStatus.ACTIVE);
    return formField;
  }
}
