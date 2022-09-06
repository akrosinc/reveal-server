package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.annotation.AllowedSortProperties;
import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.api.v1.dto.response.CountResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.enums.SummaryEnum;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.util.UserUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/location")
public class LocationController {

  private final LocationService locationService;
  private final LocationRelationshipService locationRelationshipService;

  @Operation(summary = "Create a Location",
      description = "Create a Location",
      tags = {"Location"}
  )
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<LocationResponse> createLocation(
      @Valid @RequestBody LocationRequest locationRequest) throws Exception {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(LocationResponseFactory.fromEntity(locationService.createLocation(locationRequest)));
  }

  @Operation(summary = "Fetch a Location by Identifier",
      description = "Fetch a Location by Identifier",
      tags = {"Location"}
  )
  @GetMapping(value = "/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<LocationResponse> findLocationById(
      @Parameter(description = "Location Identifier") @PathVariable UUID identifier) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(LocationResponseFactory.fromEntityWithChildCount(
            locationService.findByIdentifier(identifier),
            locationRelationshipService.getNumberOfChildren(identifier)));
  }

  @Operation(summary = "Search for Locations",
      description = "Search for Locations",
      tags = {"Location"}
  )
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> getLocations(@AllowedSortProperties(value = {"name"}) Pageable pageable,
      @Parameter(description = "Location Search parameter") @RequestParam(defaultValue = "") String search,
      @Parameter(description = "Toggle summary data") @RequestParam(defaultValue = "true", required = false) SummaryEnum _summary) {
    if (_summary.equals(SummaryEnum.COUNT)) {
      return ResponseEntity.status(HttpStatus.OK)
          .body(new CountResponse(locationService.getAllCount(search)));
    } else {
      return ResponseEntity.status(HttpStatus.OK)
          .body(LocationResponseFactory
              .fromEntityPage(locationService.getLocations(search, pageable), pageable,
                  _summary));

    }
  }

  @Operation(summary = "Update Location", description = "Update Location", tags = {"Location"})
  @PutMapping(value = "/{identifier}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<LocationResponse> updateLocation(
      @Parameter(description = "Location identifier") @PathVariable UUID identifier,
      @Valid @RequestBody LocationRequest locationRequest) {
    return ResponseEntity.status(HttpStatus.OK).body(LocationResponseFactory
        .fromEntity(locationService.updateLocation(identifier, locationRequest)));
  }


  @Operation(summary = "Delete Location", description = "Delete Location", tags = {"Location"})
  @DeleteMapping("/{identifier}")
  public ResponseEntity<Void> deleteLocation(
      @Parameter(description = "Location identifier") @PathVariable UUID identifier) {
    locationService.deleteLocation(identifier);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Operation(summary = "Location Children", description = "Retrieve Locations By Parent Identifier", tags = {
      "Location"})
  @GetMapping("/{parentLocationIdentifier}/children/{planIdentifier}")
  public ResponseEntity<List<LocationResponse>> getLocationByParentIdentifier(
      @PathVariable UUID parentLocationIdentifier, @PathVariable UUID planIdentifier) {
    return ResponseEntity.ok(locationService.getLocationsByParentIdentifierAndPlanIdentifier(
        parentLocationIdentifier, planIdentifier).stream().map(
        planLocationDetails -> LocationResponseFactory.fromPlanLocationDetails(planLocationDetails,
            parentLocationIdentifier)
    ).collect(Collectors.toList()));
  }

  @GetMapping("/{locationIdentifier}/{planIdentifier}")
  public ResponseEntity<LocationResponse> getLocationDetailsByIdentifierAndPlanIdentifier(
      @PathVariable UUID locationIdentifier, @PathVariable UUID planIdentifier) {
    PlanLocationDetails planLocationDetails = locationService.getLocationDetailsByIdentifierAndPlanIdentifier(
        locationIdentifier, planIdentifier);
    planLocationDetails.setChildrenNumber(
        locationRelationshipService.getNumberOfChildren(locationIdentifier));
    return ResponseEntity.status(HttpStatus.OK)
        .body(LocationResponseFactory.fromPlanLocationDetails(planLocationDetails, null));
  }

  @GetMapping("/download/{hierarchyIdentifier}/{geographicLevelName}")
  public ResponseEntity<?> downloadLocations(@PathVariable UUID hierarchyIdentifier,
      @PathVariable String geographicLevelName, @RequestParam ArrayList<UUID> entityTags)
      throws IOException {
    UUID userId = UUID.fromString(UserUtils.getCurrentPrinciple().getName());
    return ResponseEntity.status(HttpStatus.OK)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-disposition", "attachment;filename=Location.xlsx")
        .body(locationService.downloadLocations(hierarchyIdentifier, geographicLevelName, userId,
            entityTags));
  }

  @GetMapping("/refresh-counts")
  public ResponseEntity<?> refresh()
      throws IOException {
    locationRelationshipService.refreshLocationCountsView();
    locationRelationshipService.refreshLiteStructureCountView();
    return ResponseEntity.ok("refreshed requested");
  }
}
