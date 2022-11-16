package com.revealprecision.revealserver.api.v1.facade;

import com.revealprecision.revealserver.api.v1.facade.factory.PlanFacadeFactory;
import com.revealprecision.revealserver.api.v1.facade.models.PlanFacade;
import com.revealprecision.revealserver.api.v1.facade.request.PlanRequestFacade;
import com.revealprecision.revealserver.api.v1.facade.service.PlanFacadeService;
import com.revealprecision.revealserver.persistence.domain.Plan;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/rest")
@Profile("Running")
public class PlanFacadeController {

  private final PlanFacadeService planFacadeService;

  @Operation(summary = "Used by reveal-client to fetch Plans", description = "Fetching plans by reveal-client for certain organizations ", tags = {
      "UserContext"})
  @PostMapping("/plans/sync")
  public ResponseEntity<List<PlanFacade>> fetchPlans(
      @RequestBody PlanRequestFacade planRequestFacade) {
    Set<Plan> plans = planFacadeService.getPlans(planRequestFacade);
    return ResponseEntity
        .status(HttpStatus.OK)
        .header("total_records", Integer.toString(plans.size()))
        .body(plans
            .stream()
            .map(PlanFacadeFactory::fromEntity).collect(
                Collectors.toList()));
  }
}