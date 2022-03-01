package com.revealprecision.revealserver.api.v1.facade.controller;

import com.revealprecision.revealserver.api.v1.facade.request.LocationSyncRequest;
import io.swagger.v3.oas.annotations.Operation;
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
public class LocationFacadeController {
  public static final String  TOTAL_RECORDS = "total_records";

  @Operation(summary = "Sync Locations for Android app", description = "Sync Locations for Android app", tags = {
      "Location Sync Facade"})
  @PostMapping(value = "/sync", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getLocations(@RequestBody LocationSyncRequest locationSyncRequest) {
    boolean returnCount = locationSyncRequest.isReturnCount();
    HttpHeaders headers = new HttpHeaders();
    if(returnCount){
      Long structureCount = null;//calculate structure count
      headers.add(TOTAL_RECORDS,String.valueOf(structureCount));
    }
    return ResponseEntity.status(HttpStatus.OK).headers(headers).body("");
  }


}
