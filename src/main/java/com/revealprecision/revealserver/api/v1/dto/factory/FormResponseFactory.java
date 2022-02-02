package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.FormResponse;
import com.revealprecision.revealserver.persistence.domain.Form;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FormResponseFactory {

  public static FormResponse fromEntity(Form form) {
    return FormResponse.builder()
        .identifier(form.getIdentifier())
        .name(form.getName())
        .title(form.getTitle())
        .template(form.isTemplate())
        .payload(form.getPayload())
        .build();
  }

  public static Page<FormResponse> fromEntityPage(Page<Form> forms, Pageable pageable) {
    var response = forms.getContent().stream()
        .map(FormResponseFactory::fromEntity)
        .collect(Collectors.toList());
    return new PageImpl<>(response, pageable, forms.getTotalElements());
  }
}
