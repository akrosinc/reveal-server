package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.FormResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.FormRequest;
import com.revealprecision.revealserver.api.v1.dto.request.FormUpdateRequest;
import com.revealprecision.revealserver.api.v1.dto.response.FormResponse;
import com.revealprecision.revealserver.enums.FlagEnum;
import com.revealprecision.revealserver.service.FormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

//TODO: Not Used
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/form")
public class FormController {

  private final FormService formService;

  @Operation(summary = "Create form",
      description = "Create form",
      tags = {"Form"}
  )
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> createForm(@Valid @RequestBody FormRequest formRequest) {
    formService.createFrom(formRequest);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @Operation(summary = "Get form by Identifier",
      description = "Get form by Identifier",
      tags = {"Form"}
  )
  @GetMapping(value = "/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<FormResponse> getByIdentifier(@PathVariable("identifier") UUID identifier) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(FormResponseFactory.fromEntity(formService.findById(identifier)));
  }

  @Operation(summary = "Get all forms by criteria",
      description = "Get all forms by criteria",
      tags = {"Form"}
  )
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Page<FormResponse>> getAll(Pageable pageable,
      @Parameter(description = "Is form template?") @RequestParam(name = "template", required = false) FlagEnum template,
      @Parameter(description = "Parameter searching by name and title") @RequestParam(name = "search", defaultValue = "", required = false) String search) {
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(FormResponseFactory
            .fromEntityPage(formService.getAll(search, pageable, template),
                pageable));
  }

  @Operation(summary = "Get all forms by criteria",
      description = "Get all forms by criteria",
      tags = {"Form"}
  )
  @GetMapping(value = "/dropdown", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<FormResponse>> getAllForDropdown() {
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(
            formService.getAllForDropdown().stream().map(FormResponseFactory::fromEntityForDropdown)
                .collect(
                    Collectors.toList()));
  }

  @Operation(summary = "Update form",
      description = "Update form",
      tags = {"Form"}
  )
  @PutMapping(value = "/{identifier}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> updateForm(@Valid @RequestBody FormUpdateRequest formUpdateRequest,
      @Parameter(description = "Form identifier") @PathVariable("identifier") UUID identifier) {
    formService.updateForm(formUpdateRequest, identifier);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @Operation(summary = "Delete form",
      description = "Delete form",
      tags = {"Form"}
  )
  @DeleteMapping(value = "/{identifier}")
  public ResponseEntity<Void> deleteForm(
      @Parameter(description = "Form identifier") @PathVariable("identifier") UUID identifier) {
    formService.deleteForm(identifier);
    return ResponseEntity.status(HttpStatus.OK).build();
  }
}
