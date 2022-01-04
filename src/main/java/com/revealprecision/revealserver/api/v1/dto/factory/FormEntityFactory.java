package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.FormRequest;
import com.revealprecision.revealserver.persistence.domain.Form;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FormEntityFactory {

  public static Form toEntity(FormRequest formRequest) {
    return Form.builder()
        .name(formRequest.getName())
        .template(formRequest.isTemplate())
        .title(formRequest.getTitle())
        .payload(formRequest.getPayload())
        .build();
  }
}
