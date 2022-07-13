package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.FormFieldResponse;
import com.revealprecision.revealserver.enums.ActionTitleEnum;
import com.revealprecision.revealserver.persistence.domain.FormField;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FormFieldResponseFactory {


  public static FormFieldResponse fromEntity(FormField formField) {

    return FormFieldResponse.builder()
        .identifier(formField.getIdentifier())
        .addToMetadata(formField.isAddToMetadata())
        .display(formField.getDisplay())
        .name(formField.getName())
        .dataType(formField.getDataType())
        .formTitle(formField.getFormTitle())
        .build();
  }

}
