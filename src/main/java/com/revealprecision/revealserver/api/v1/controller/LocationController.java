package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
@RequestMapping("/api/v1/location")
public class LocationController {

  private LocationService locationService;

  @Autowired
  public LocationController(LocationService locationService) {
    this.locationService = locationService;
  }

  @Operation(summary = "Create a Location",
      description = "Create a Location",
      tags = {"Location"}
  )
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<LocationResponse> createLocation(
      @Valid @RequestBody LocationRequest locationRequest) {
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
        .body(LocationResponseFactory.fromEntity(locationService.findByIdentifier(identifier)));
  }

  @Operation(summary = "Search for locationHierarchy",
      description = "Search for locationHierarchy",
      tags = {"Location"}
  )
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Page<Location> getLocations(@RequestParam(defaultValue = "0") Integer pageNumber,
      @RequestParam(defaultValue = "50") Integer pageSize) {
    return locationService.getLocations(pageNumber, pageSize);
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
}
