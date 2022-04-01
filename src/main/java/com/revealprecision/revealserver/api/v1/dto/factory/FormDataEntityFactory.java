package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.FormDataRequest;
import com.revealprecision.revealserver.persistence.domain.Form;
import com.revealprecision.revealserver.persistence.domain.FormData;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FormDataEntityFactory {

  public static FormData toEntity(FormDataRequest formDataRequest, Form form) {
    return FormData.builder().payload(formDataRequest.getPayload()).form(form).build();
  }
}
