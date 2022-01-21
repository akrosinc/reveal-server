package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationBulkDetailResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.LocationBulkResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.response.IdentifierResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationBulkDetailResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationBulkResponse;
import com.revealprecision.revealserver.batch.runner.LocationBatchRunner;
import com.revealprecision.revealserver.service.LocationBulkService;
import com.revealprecision.revealserver.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/location/bulk")
@CrossOrigin(originPatterns = "*", origins = "*")
public class LocationBulkController {

  private final LocationBulkService locationBulkService;
  private final LocationBatchRunner locationBatchRunner;
  private final StorageService storageService;


  @Operation(summary = "Import Locations from JSON file",
      description = "Import Locations from JSON file",
      tags = {"Bulk Location"}
  )
  @PostMapping()
  public ResponseEntity<IdentifierResponse> importLocations(
      @RequestParam("file") MultipartFile file)
      throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {

    String path = storageService.saveJSON(file);
    UUID identifier = locationBulkService.saveBulk(file.getOriginalFilename());
    locationBatchRunner.run(identifier.toString(), path);
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(IdentifierResponse.builder().identifier(identifier).build());
  }

  @Operation(summary = "Get List of LocationBulk operations",
      description = "Get List of LocationBulk operations",
      tags = {"Bulk Location"})
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Page<LocationBulkResponse>> getLocationBulks(Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK).body(LocationBulkResponseFactory
        .fromEntityPage(locationBulkService.getLocationBulks(pageable), pageable));
  }

  @Operation(summary = "Download .json sample file for Location import",
      description = "Download .json sample file for Location import",
      tags = {"Location Bulk"}
  )
  @GetMapping("/sample")
  public ResponseEntity<?> downloadTemplate() throws IOException {
    return ResponseEntity.status(HttpStatus.OK)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-disposition", "attachment;filename=LocationUploadSample.json")
        .body(storageService.downloadTemplate("LocationUploadSample.json"));
  }


  @Operation(summary = "Get Location Bulk details",
      description = "Get Location Bulk details",
      tags = {"User bulk"}
  )
  @GetMapping("/{identifier}")
  public ResponseEntity<Page<LocationBulkDetailResponse>> getBulkDetails(
      @Parameter(description = "Location Bulk identifier") @PathVariable("identifier") UUID identifier,
      Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK).body(LocationBulkDetailResponseFactory
        .fromProjectionPage(locationBulkService.getLocationBulkDetails(identifier, pageable),
            pageable));
  }
}
