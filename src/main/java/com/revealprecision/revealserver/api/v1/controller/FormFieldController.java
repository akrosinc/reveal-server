package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.FormFieldResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.FormFieldRequest;
import com.revealprecision.revealserver.api.v1.dto.response.FormFieldResponse;
import com.revealprecision.revealserver.service.FormFieldService;
import io.swagger.v3.oas.annotations.Operation;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/form-field")
@Profile("Running")
public class FormFieldController {

  private final FormFieldService formFieldService;

  @Operation(summary = "Create Form Field", description = "Create Form Field", tags = {
      "Form Fields"})
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<FormFieldResponse> createFormField(
      @Valid @RequestBody FormFieldRequest formFieldRequest) {
    return ResponseEntity.status(HttpStatus.CREATED).body(
        FormFieldResponseFactory.fromEntity(formFieldService.createFormField(formFieldRequest)));
  }

}
