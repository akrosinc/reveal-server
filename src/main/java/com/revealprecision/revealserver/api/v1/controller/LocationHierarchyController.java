package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.service.LocationHierarchyService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/")
public class LocationHierarchyController {

  private LocationHierarchyService locationHierarchyService;

  @Autowired
  public LocationHierarchyController(LocationHierarchyService locationHierarchyService) {
    this.locationHierarchyService = locationHierarchyService;
  }

  @Operation(summary = "Create a locationHierarchy",
      description = "Create a locationHierarchy",
      tags = {"Location"}
  )
  @PostMapping(value = "/locationHierarchy", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public LocationHierarchy create(@Validated @RequestBody LocationHierarchy locationHierarchy) {
    return locationHierarchyService.createLocationHierarchy(locationHierarchy);
  }

}