package com.revealprecision.revealserver.api.v1.facade.controller;

import com.revealprecision.revealserver.api.v1.facade.factory.PhysicalLocationResponseFactory;
import com.revealprecision.revealserver.api.v1.facade.models.PhysicalLocation;
import com.revealprecision.revealserver.api.v1.facade.request.LocationSyncRequest;
import com.revealprecision.revealserver.api.v1.facade.service.LocationFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/location")
@RequiredArgsConstructor
public class LocationFacadeController {

  private final LocationFacadeService locationFacadeService;

  @Operation(summary = "Sync Locations for Android app", description = "Sync Locations for Android app", tags = {
      "Location Sync Facade"})
  @PostMapping(value = "/sync", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<PhysicalLocation>> getLocations(
      @RequestBody LocationSyncRequest locationSyncRequest) {
    List<PhysicalLocation> physicalLocations;
    HttpHeaders headers = new HttpHeaders();
    physicalLocations = PhysicalLocationResponseFactory
        .fromLocationsAndRelationship(locationFacadeService.syncLocations(locationSyncRequest),
            new ArrayList<>()); //TODO: how do determine hierarchy
    headers = locationFacadeService.addCountToHeaders(physicalLocations.stream().count(), headers);
    return ResponseEntity.status(HttpStatus.OK).headers(headers).body(physicalLocations);
  }


}
