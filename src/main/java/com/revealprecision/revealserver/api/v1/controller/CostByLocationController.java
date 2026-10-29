package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.request.CostByLocationRequest;
import com.revealprecision.revealserver.api.v1.dto.response.CostByLocationResponse;
import com.revealprecision.revealserver.service.CostByLocationService;
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
@RequestMapping("/api/v1/cost-by-location")
public class CostByLocationController {

  private final CostByLocationService costByLocationService;

  @GetMapping("/{identifier}")
  public ResponseEntity<CostByLocationResponse> getByIdentifier(
      @PathVariable("identifier") UUID identifier) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(CostByLocationResponse.fromEntity(costByLocationService.getByIdentifier(identifier)));
  }

  @GetMapping
  public ResponseEntity<List<CostByLocationResponse>> search(
      @Parameter(description = "Location identifier") @RequestParam(name = "locationIdentifier", required = false) UUID locationIdentifier,
      @Parameter(description = "Action identifier") @RequestParam(name = "actionIdentifier", required = false) UUID actionIdentifier) {
    List<CostByLocationResponse> responses;
    if (locationIdentifier != null) {
      responses = costByLocationService.getByLocation(locationIdentifier).stream()
          .map(CostByLocationResponse::fromEntity).collect(Collectors.toList());
    } else if (actionIdentifier != null) {
      responses = costByLocationService.getByAction(actionIdentifier).stream()
          .map(CostByLocationResponse::fromEntity).collect(Collectors.toList());
    } else {
      responses = List.of();
    }
    return ResponseEntity.status(HttpStatus.OK).body(responses);
  }

  @PostMapping
  public ResponseEntity<CostByLocationResponse> create(
      @Valid @RequestBody CostByLocationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(CostByLocationResponse.fromEntity(costByLocationService.create(request)));
  }

  @PutMapping("/{identifier}")
  public ResponseEntity<CostByLocationResponse> update(
      @PathVariable("identifier") UUID identifier,
      @Valid @RequestBody CostByLocationRequest request) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(CostByLocationResponse.fromEntity(costByLocationService.update(identifier, request)));
  }

  @DeleteMapping("/{identifier}")
  public ResponseEntity<Void> delete(@PathVariable("identifier") UUID identifier) {
    costByLocationService.delete(identifier);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
