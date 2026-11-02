package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.ActionTemplateResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.ActionTemplateRequest;
import com.revealprecision.revealserver.api.v1.dto.response.ActionTemplateResponse;
import com.revealprecision.revealserver.service.ActionTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.UUID;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/action-template")
@RequiredArgsConstructor
public class ActionTemplateController {

  private final ActionTemplateService actionTemplateService;

  @Operation(summary = "Create Action Template",
      description = "Create Action Template",
      tags = {"ActionTemplate"}
  )
  @PostMapping
  public ResponseEntity<ActionTemplateResponse> create(
      @Valid @RequestBody final ActionTemplateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ActionTemplateResponseFactory.fromEntity(actionTemplateService.create(request)));
  }

  @Operation(summary = "Fetch Action Templates",
      description = "Fetch Action Templates",
      tags = {"ActionTemplate"}
  )
  @GetMapping
  public ResponseEntity<Page<ActionTemplateResponse>> getAll(Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ActionTemplateResponseFactory.fromEntityPage(actionTemplateService.getAll(pageable),
            pageable));
  }

  @Operation(summary = "Fetch Action Template by identifier",
      description = "Fetch Action Template by identifier",
      tags = {"ActionTemplate"}
  )
  @GetMapping("/{identifier}")
  public ResponseEntity<ActionTemplateResponse> getById(
      @Parameter(description = "Action Template identifier") @PathVariable UUID identifier) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ActionTemplateResponseFactory.fromEntity(actionTemplateService.findById(identifier)));
  }

  @Operation(summary = "Update Action Template",
      description = "Update Action Template",
      tags = {"ActionTemplate"}
  )
  @PutMapping("/{identifier}")
  public ResponseEntity<ActionTemplateResponse> update(
      @Parameter(description = "Action Template identifier") @PathVariable UUID identifier,
      @Valid @RequestBody final ActionTemplateRequest request) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ActionTemplateResponseFactory.fromEntity(
            actionTemplateService.update(identifier, request)));
  }

  @Operation(summary = "Delete Action Template",
      description = "Delete Action Template",
      tags = {"ActionTemplate"}
  )
  @DeleteMapping("/{identifier}")
  public ResponseEntity<Void> delete(
      @Parameter(description = "Action Template identifier") @PathVariable UUID identifier) {
    actionTemplateService.delete(identifier);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
