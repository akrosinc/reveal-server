package com.revealprecision.revealserver.api.v1.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.repository.GeographicLevelRepository;
import com.revealprecision.revealserver.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Optional;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/")
public class LocationController {

  private LocationService locationService;
  private GeographicLevelRepository geographicLevelRepository;

  @Autowired
  public LocationController(LocationService locationService,
      GeographicLevelRepository geographicLevelRepository) {
    this.locationService = locationService;
    this.geographicLevelRepository = geographicLevelRepository;
  }

  @Operation(summary = "Create a location",
      description = "Create a location",
      tags = {"Location"}
  )
  @PostMapping(value = "/location", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<LocationResponse> createLocation(
      @Valid @RequestBody LocationRequest locationRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(LocationResponseFactory.fromEntity(locationService.createLocation(locationRequest)));
  }

  @Operation(summary = "Fetch a locationHierarchy",
          description = "Fetch a locationHierarchy",
          tags = {"Location"}
  )
  @GetMapping(value = "/location/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Object findLocationById(@PathVariable UUID identifier) {
    Optional<Location> locationOptional = locationService.findByIdentifier(identifier);
    if (locationOptional.isPresent()) {
      return locationOptional.get();
    }
    return new ResponseEntity<Location>(HttpStatus.NO_CONTENT);
  }

  @Operation(summary = "Search for locationHierarchy",
          description = "Search for locationHierarchy",
          tags = {"Location"}
  )
  @GetMapping(value = "/location", produces = MediaType.APPLICATION_JSON_VALUE)
  public Page<Location> getLocations(@RequestParam(defaultValue = "0") Integer pageNumber,
      @RequestParam(defaultValue = "50") Integer pageSize) {
    return locationService.getLocations(pageNumber, pageSize);
  }
}
