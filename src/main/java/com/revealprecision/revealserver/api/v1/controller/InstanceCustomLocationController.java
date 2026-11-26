package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.request.InstanceCustomLocationRequest;
import com.revealprecision.revealserver.api.v1.dto.request.InstanceLocationEntityTagCountRequest;
import com.revealprecision.revealserver.api.v1.dto.request.InstanceLocationEntityTagCountSearchRequest;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceLocationEntityTagCountResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.service.InstanceCustomLocationService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

/*
Instance custom locations (polygons drawn under a shared Health Facility) and their points.
The instance is the one of the X-Instance-ID header.
*/
@RestController
@RequestMapping("/api/v1/instance-custom-location")
@RequiredArgsConstructor
public class InstanceCustomLocationController {

  private final InstanceCustomLocationService instanceCustomLocationService;

  @Operation(summary = "Create a custom location with its points",
      description = "Creates the custom location and its points of the current instance in one transaction; "
          + "returns the created custom location followed by its points",
      tags = {"Instance Custom Location"})
  @PostMapping
  public ResponseEntity<List<LocationResponse>> createCustomLocation(
      @RequestBody InstanceCustomLocationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(instanceCustomLocationService.createCustomLocation(request));
  }

  @Operation(summary = "List custom locations and points under a parent location",
      description = "Custom locations of the current instance under the given location, each followed by its points",
      tags = {"Instance Custom Location"})
  @GetMapping
  public ResponseEntity<List<LocationResponse>> getCustomLocations(
      @RequestParam("parentLocationId") UUID parentLocationId) {
    return ResponseEntity.ok(instanceCustomLocationService.getCustomLocations(parentLocationId));
  }

  @Operation(summary = "Update a custom location",
      description = "Updates a custom location of the current instance; points, when sent, replace the current points",
      tags = {"Instance Custom Location"})
  @PutMapping("/{identifier}")
  public ResponseEntity<List<LocationResponse>> updateCustomLocation(@PathVariable UUID identifier,
      @RequestBody InstanceCustomLocationRequest request) {
    return ResponseEntity.ok(instanceCustomLocationService.updateCustomLocation(identifier, request));
  }

  @Operation(summary = "Delete a custom location",
      description = "Deletes a custom location of the current instance with its points. When tagName "
          + "is given, takes one off that tag on the parent location",
      tags = {"Instance Custom Location"})
  @DeleteMapping("/{identifier}")
  public ResponseEntity<Void> deleteCustomLocation(@PathVariable UUID identifier,
      @RequestParam(required = false) String tagName) {
    instanceCustomLocationService.deleteCustomLocation(identifier, tagName);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Operation(summary = "Set entity tags count for a location",
      description = "Sets the counts of numeric entity tags for a location of the current instance;"
          + "each count replaces the previous value and is written for the location and "
          + "its ancestors, like a dataset upload",
      tags = {"Instance Custom Location"})
  @PostMapping("/entity-tag-counts")
  public ResponseEntity<Void> setLocationEntityTagCounts(
      @RequestBody InstanceLocationEntityTagCountRequest request) {
    instanceCustomLocationService.setLocationEntityTagCounts(request);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @Operation(summary = "Get entity tag counts of a location",
      description = "Counts of the given entity tags for a location of the current instance: count is "
          + "the value set for the location itself, total also includes its descendants. Without "
          + "dataCaptureDate the values set without a date are returned",
      tags = {"Instance Custom Location"})
  @PostMapping("/entitytag/getcounts")
  public ResponseEntity<InstanceLocationEntityTagCountResponse> getLocationEntityTagCounts(
      @RequestBody InstanceLocationEntityTagCountSearchRequest request) {
    return ResponseEntity.ok(instanceCustomLocationService.getLocationEntityTagCounts(
        request.getLocationId(), request.getTags(), request.getDataCaptureDate()));
  }
}
