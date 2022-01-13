package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.GeographicLevelResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.GeographicLevelRequest;
import com.revealprecision.revealserver.api.v1.dto.response.GeographicLevelResponse;
import com.revealprecision.revealserver.service.GeographicLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/geographicLevel")
public class GeographicLevelController {

  private GeographicLevelService geographicLevelService;

  @Autowired
  public GeographicLevelController(GeographicLevelService geographicLevelService) {
    this.geographicLevelService = geographicLevelService;
  }

  @Operation(summary = "Create a geographicLevel",
      description = "Create a geographicLevel",
      tags = {"Geographic Level"}
  )
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<GeographicLevelResponse> create(
      @Valid @RequestBody GeographicLevelRequest geographicLevelRequest) {
    return ResponseEntity.status(HttpStatus.CREATED).body(GeographicLevelResponseFactory.fromEntity(
        geographicLevelService.createGeographicLevel(
            geographicLevelRequest)));
  }

  @Operation(summary = "List geographicLevels",
      description = "List geographicLevels",
      tags = {"Geographic Level"}
  )
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Page<GeographicLevelResponse>> getGeographicLevels(
      @PageableDefault(size = 50)
          Pageable pageable) {

    return ResponseEntity.status(HttpStatus.OK).body(
        GeographicLevelResponseFactory.fromEntityPage(
            geographicLevelService.getGeographicLevels(pageable), pageable));
  }

  @Operation(summary = "Fetch a geographicLevel",
      description = "Fetch a geographicLevel",
      tags = {"Geographic Level"}
  )
  @GetMapping(value = "/geographicLevel/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<GeographicLevelResponse> findGeographicLevelByIdentifier(
      @Parameter(description = "Identifier of the geographicLevel") @PathVariable UUID identifier) {
    return ResponseEntity.status(HttpStatus.OK).body(GeographicLevelResponseFactory.fromEntity(
        geographicLevelService.findGeographicLevelByIdentifier(identifier)));
  }

  @Operation(summary = "Update a geographicLevel",
      description = "Update a geographicLevel",
      tags = {"Geographic Level"}
  )
  @PutMapping(value = "/{identifier}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<GeographicLevelResponse> updateGeographicLevel(
      @Valid @RequestBody GeographicLevelRequest geographicLevelRequest,
      @Parameter(description = "Identifier of the geographicLevel") @PathVariable UUID identifier) {
    return ResponseEntity.status(HttpStatus.OK).body(GeographicLevelResponseFactory.fromEntity(
        geographicLevelService.update(identifier,
            geographicLevelRequest)));
  }

  @Operation(summary = "Delete a geographicLevel",
      description = "Delete a geographicLevel",
      tags = {"Geographic Level"}
  )
  @DeleteMapping(value = "/{identifier}")
  public ResponseEntity<Void> deleteGeographicLeve(@PathVariable UUID identifier) {
    geographicLevelService.deleteGeographicLevel(identifier);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

}
