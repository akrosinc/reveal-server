package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.OrganizationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.OrganizationRequest;
import com.revealprecision.revealserver.api.v1.dto.response.OrganizationResponse;
import com.revealprecision.revealserver.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

@RestController
@RequestMapping("/api/v1/organization")
public class OrganizationController {

  private OrganizationService organizationService;

  @Autowired
  public OrganizationController(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  @Operation(summary = "Create an organization",
      description = "Create an organization",
      tags = {"Organization"}
  )
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<OrganizationResponse> createOrganization(
      @RequestBody OrganizationRequest organizationRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(OrganizationResponseFactory.fromEntityWithoutChild(
            organizationService.createOrganization(organizationRequest)));
  }

  @Operation(summary = "Get an organization by identifier",
      description = "Get an organization by identifier",
      tags = {"Organization"}
  )
  @GetMapping(value = "/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<OrganizationResponse> getOrganization(@PathVariable UUID identifier,
      @RequestParam(defaultValue = "true", required = false) boolean _summary) {
    return ResponseEntity
        .status(HttpStatus.OK)
        .body((_summary) ? OrganizationResponseFactory.fromEntityWithoutChild(
            organizationService.findById(identifier, _summary))
            : OrganizationResponseFactory.fromEntityWithChild(
                organizationService.findById(identifier, _summary)));
  }

  @PutMapping(value = "/{identifier}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<OrganizationResponse> updateOrganization(
      @Valid @RequestBody OrganizationRequest organizationRequest, @PathVariable UUID identifier) {
    return ResponseEntity.status(HttpStatus.OK).body(
        OrganizationResponseFactory.fromEntityWithoutChild(
            organizationService.updateOrganization(identifier, organizationRequest)));
  }

  @DeleteMapping(value = "/{identifier}")
  public ResponseEntity<Void> deleteOrganization(@PathVariable UUID identifier) {
    organizationService.deleteOrganization(identifier);
    return ResponseEntity.ok().build();
  }
}
