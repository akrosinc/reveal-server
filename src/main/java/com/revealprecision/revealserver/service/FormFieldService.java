package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.FormFieldFactory;
import com.revealprecision.revealserver.api.v1.dto.request.FormFieldRequest;
import com.revealprecision.revealserver.persistence.domain.FormField;
import com.revealprecision.revealserver.persistence.repository.FormFieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FormFieldService {

  private final FormFieldRepository formFieldRepository;

  public FormField findByNameAndFormTitle(String name, String formTitle) {
    return formFieldRepository.findByNameAndFormTitle(name, formTitle);
  }

  public FormField createFormField(FormFieldRequest formFieldRequest) {

    return formFieldRepository.save(FormFieldFactory.toEntity(formFieldRequest));
  }
}
