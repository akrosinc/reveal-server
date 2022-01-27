package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.request.LookupInterventionTypeRequest;
import com.revealprecision.revealserver.service.LookupInterventionTypeService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/lookupInterventionType")
public class LookupInterventionTypeController {

  private final LookupInterventionTypeService interventionTypeService;

  @PostMapping
  public ResponseEntity<Void> createInterventionType(@Valid @RequestBody
      LookupInterventionTypeRequest request) {
    interventionTypeService.createInterventionType(request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}
