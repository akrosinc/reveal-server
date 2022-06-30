package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.annotation.AllowedSortProperties;
import com.revealprecision.revealserver.api.v1.dto.factory.LocationHierarchyResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.LocationHierarchyRequest;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationHierarchyResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.enums.SummaryEnum;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.projection.LocationMainData;
import com.revealprecision.revealserver.service.LocationHierarchyService;
import com.revealprecision.revealserver.service.LocationRelationshipService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/locationHierarchy")
public class LocationHierarchyController {

  private final LocationHierarchyService locationHierarchyService;
  private final LocationRelationshipService locationRelationshipService;

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
    //TODO: we need to update specification for this endpoint and it's intention. which might cause this to change
    var locationHierarchy = locationHierarchyService.findByIdentifier(identifier);
    return ResponseEntity.status(HttpStatus.OK).body((_summary) ? LocationHierarchyResponseFactory
        .fromEntityWithoutTree(locationHierarchy)
        : LocationHierarchyResponseFactory.fromEntityWithTree(locationHierarchy, true));
  }


  @Operation(summary = "Get Locations  and their children for given Hierarchy by identifier",
      description = "Get Locations and their children for given Hierarchy by identifier",
      tags = {"Location Hierarchy"}
  )
  @GetMapping("/{identifier}/location")
  public ResponseEntity<Page<GeoTreeResponse>> getLocationsForHierarchy(
      @Parameter(description = "LocationHierarchy identifier") @PathVariable UUID identifier,
      Pageable pageable,
      @Parameter(description = "Location Search parameter") @RequestParam(defaultValue = "") String search,
      @Parameter(description = "Toggle summary data") @RequestParam(defaultValue = "TRUE", required = false) SummaryEnum _summary) {
    LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(identifier);
    Boolean includeGeometry = _summary.equals(SummaryEnum.FALSE);
    List<GeoTreeResponse> geoTreeResponses = locationHierarchyService.getGeoTreeFromLocationHierarchy(locationHierarchy);
    Page<GeoTreeResponse> pageableGeoTreeResponse = LocationHierarchyResponseFactory.generatePageableGeoTreeResponse(geoTreeResponses,pageable, search);
    return ResponseEntity.status(HttpStatus.OK).body(pageableGeoTreeResponse);
  }

  @GetMapping("/{identifier}/location/{locationIdentifier}")
  public ResponseEntity<List<LocationResponse>> getChildLocations(@Parameter(description = "LocationHierarchy identifier") @PathVariable UUID identifier,
      @Parameter(description = "Location identifier") @PathVariable UUID locationIdentifier) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(locationRelationshipService.getChildrenLocations(identifier, locationIdentifier).stream()
            .map(LocationResponseFactory::fromEntitySummary)
            .collect(Collectors.toList()));
  }


  @Operation(summary = "Get List of Location Hierarchy",
      description = "Get List of Location Hierarchy",
      tags = {"Location Hierarchy"}
  )
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Page<LocationHierarchyResponse>> getLocationHierarchies(
      @AllowedSortProperties(value = {"name"}) Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK).body(LocationHierarchyResponseFactory
        .fromEntityPage(locationHierarchyService.getLocationHierarchies(pageable), pageable));
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

  @GetMapping("/{identifier}/{levelName}")
      public ResponseEntity<List<LocationMainData>> getLocationsByHierarchyIdAndLevelName(@PathVariable UUID identifier, @PathVariable String levelName) {
    return ResponseEntity.status(HttpStatus.OK).body(locationRelationshipService.getLocationsByHierarchyIdAndLevelName(identifier, levelName));
  }
}
