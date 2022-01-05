package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationHierarchyResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.LocationHierarchyRequest;
import com.revealprecision.revealserver.api.v1.dto.response.LocationHierarchyResponse;
import com.revealprecision.revealserver.service.LocationHierarchyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/locationHierarchy")
public class LocationHierarchyController {

  private LocationHierarchyService locationHierarchyService;

  @Autowired
  public LocationHierarchyController(LocationHierarchyService locationHierarchyService) {
    this.locationHierarchyService = locationHierarchyService;
  }

  @Operation(summary = "Create a locationHierarchy",
      description = "Create a locationHierarchy",
      tags = {"Location Hierarchy"}
  )
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<LocationHierarchyResponse> createLocationHierarchy(
      @Valid @RequestBody LocationHierarchyRequest locationHierarchyRequest) {
    return ResponseEntity.status(HttpStatus.CREATED).body(LocationHierarchyResponseFactory
        .fromEntityWithoutTree(
            locationHierarchyService.createLocationHierarchy(locationHierarchyRequest)));
  }

  @Operation(summary = "Get LocationHierarchy by identifier",
      description = "Get LocationHierarchy by identifier",
      tags = {"Location Hierarchy"}
  )
  @GetMapping("/{identifier}")
  public ResponseEntity<LocationHierarchyResponse> getLocationHierarchy(
      @Parameter(description = "LocationHierarchy identifier") @PathVariable UUID identifier,
      @Parameter(description = "Toggle summary data") @RequestParam(defaultValue = "true", required = false) boolean _summary) {
    var locationHierarchy = locationHierarchyService.findByIdentifier(identifier);
    return ResponseEntity.status(HttpStatus.OK).body((_summary) ? LocationHierarchyResponseFactory
        .fromEntityWithoutTree(locationHierarchy)
        : LocationHierarchyResponseFactory.fromEntityWithTree(locationHierarchy));
  }

  @Operation(summary = "Delete LocationHierarchy",
      description = "Delete LocationHierarchy",
      tags = {"Location Hierarchy"}
  )
  @DeleteMapping(value = "/{identifier}")
  public ResponseEntity<Void> deleteLocationHierarchy(
      @Parameter(description = "LocationHierarchy Identifier") @PathVariable UUID identifier) {
    locationHierarchyService.deleteLocationHierarchyAndAssociatedLocationRelationships(identifier);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
