package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.request.ActionLocationRecaptureLimitRequest;
import com.revealprecision.revealserver.api.v1.dto.response.ActionLocationRecaptureLimitResponse;
import com.revealprecision.revealserver.service.ActionLocationRecaptureLimitService;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/action-location-recapture-limit")
public class ActionLocationRecaptureLimitController {

  private final ActionLocationRecaptureLimitService actionLocationRecaptureLimitService;

  @GetMapping("/{identifier}")
  public ResponseEntity<ActionLocationRecaptureLimitResponse> getByIdentifier(
      @PathVariable("identifier") UUID identifier) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ActionLocationRecaptureLimitResponse.fromEntity(
            actionLocationRecaptureLimitService.getByIdentifier(identifier)));
  }

  @GetMapping
  public ResponseEntity<List<ActionLocationRecaptureLimitResponse>> search(
      @Parameter(description = "Instance identifier") @RequestParam(name = "instanceIdentifier", required = false) UUID instanceIdentifier,
      @Parameter(description = "Location identifier") @RequestParam(name = "locationIdentifier", required = false) UUID locationIdentifier,
      @Parameter(description = "Action identifier") @RequestParam(name = "actionIdentifier", required = false) UUID actionIdentifier) {
    List<ActionLocationRecaptureLimitResponse> responses;
    if (instanceIdentifier != null && locationIdentifier != null) {
      responses = actionLocationRecaptureLimitService
          .getByInstanceAndLocation(instanceIdentifier, locationIdentifier).stream()
          .map(ActionLocationRecaptureLimitResponse::fromEntity).collect(Collectors.toList());
    } else if (instanceIdentifier != null) {
      responses = actionLocationRecaptureLimitService.getByInstance(instanceIdentifier).stream()
          .map(ActionLocationRecaptureLimitResponse::fromEntity).collect(Collectors.toList());
    } else if (locationIdentifier != null) {
      responses = actionLocationRecaptureLimitService.getByLocation(locationIdentifier).stream()
          .map(ActionLocationRecaptureLimitResponse::fromEntity).collect(Collectors.toList());
    } else if (actionIdentifier != null) {
      responses = actionLocationRecaptureLimitService.getByAction(actionIdentifier).stream()
          .map(ActionLocationRecaptureLimitResponse::fromEntity).collect(Collectors.toList());
    } else {
      responses = List.of();
    }
    return ResponseEntity.status(HttpStatus.OK).body(responses);
  }

  @PostMapping
  public ResponseEntity<ActionLocationRecaptureLimitResponse> create(
      @Valid @RequestBody ActionLocationRecaptureLimitRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ActionLocationRecaptureLimitResponse.fromEntity(
            actionLocationRecaptureLimitService.create(request.getInstanceIdentifier(),
                request.getLocationIdentifier(), request.getActionIdentifier(),
                request.getCount())));
  }

  @PutMapping("/{identifier}")
  public ResponseEntity<ActionLocationRecaptureLimitResponse> update(
      @PathVariable("identifier") UUID identifier,
      @Valid @RequestBody ActionLocationRecaptureLimitRequest request) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ActionLocationRecaptureLimitResponse.fromEntity(
            actionLocationRecaptureLimitService.update(identifier, request.getInstanceIdentifier(),
                request.getLocationIdentifier(), request.getActionIdentifier(),
                request.getCount())));
  }

  @DeleteMapping("/{identifier}")
  public ResponseEntity<Void> delete(@PathVariable("identifier") UUID identifier) {
    actionLocationRecaptureLimitService.delete(identifier);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
