package com.revealprecision.revealserver.api.v1.facade.controller;

import com.revealprecision.revealserver.api.v1.facade.models.CreateLocationRequest;
import com.revealprecision.revealserver.api.v1.facade.models.PhysicalLocation;
import com.revealprecision.revealserver.api.v1.facade.request.LocationSyncRequest;
import com.revealprecision.revealserver.api.v1.facade.service.LocationFacadeService;
import com.revealprecision.revealserver.api.v1.facade.service.LocationHierarchyFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/location")
@RequiredArgsConstructor
public class LocationFacadeController {

  private final LocationFacadeService locationFacadeService;
  private final LocationHierarchyFacadeService locationHierarchyFacadeService;
  public static final String IS_JURISDICTION = "is_jurisdiction";
  private static final String FALSE = "false";


  @Operation(summary = "Sync Locations for Reveal mobile app", description = "Sync Locations for Reveal mobile app", tags = {
      "Location"})
  @PostMapping(value = "/sync", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<PhysicalLocation>> getLocations(
      @RequestBody LocationSyncRequest locationSyncRequest) {

    UUID hierarchyIdentifier = locationHierarchyFacadeService.getIdRequestedOrReturnDefault(
        locationSyncRequest);

    List<PhysicalLocation> physicalLocations = locationFacadeService
        .syncLocations(locationSyncRequest, hierarchyIdentifier);

    HttpHeaders headers = new HttpHeaders();
    headers = locationFacadeService.addCountToHeaders(physicalLocations.stream().count(), headers);
    return ResponseEntity.status(HttpStatus.OK).headers(headers).body(physicalLocations);
  }

  @Operation(summary = "Update server with structure/Location created from reveal mobile app", description = "Update server with structure/Location created from reveal mobile app", tags = {
      "Location"})
  @PostMapping(value = "/add", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> addSyncedStructures(
      @RequestBody List<CreateLocationRequest> locations,
      @RequestParam(value = IS_JURISDICTION, defaultValue = FALSE, required = false) boolean isJurisdiction) {
    Set<String> locationRequestsWithErrors = locationFacadeService
        .saveSyncedLocations(locations);
    String message = locationRequestsWithErrors.isEmpty() ? "All Locations  processed"
        : ("Locations with Ids not processed: " + String.join(",", locationRequestsWithErrors));
    return ResponseEntity.status(HttpStatus.CREATED).body(message);
  }

}
