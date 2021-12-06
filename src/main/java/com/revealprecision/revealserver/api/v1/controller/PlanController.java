package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/")
public class PlanController {

  private PlanService planService;

  @Autowired
  public PlanController(PlanService planService) {
    this.planService = planService;
  }

  @Operation(summary = "Fetch all Plans",
      description = "Fetch all Plans",
      tags = {"Plan"}
  )
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/plan",
      produces = "application/json"
  )
  public Page<Plan> getPlans(
      @Parameter(description = "Page number to return") @RequestParam(defaultValue = "0", required = false) Integer pageNumber,
      @Parameter(description = "Number of records per page") @RequestParam(defaultValue = "50", required = false) Integer pageSize) {
    return planService.getPlans(pageNumber, pageSize);
  }

  @Operation(summary = "Fetch a Plan by identfier",
      description = "Fetch a Plan by identfier",
      tags = {"Plan"}
  )
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/plan/{identifier}",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Plan getPlanByIdentifier(
      @Parameter(description = "Plan identifier") @PathVariable("identifier") UUID planIdentifier) {
    return planService.getPlanByIdentifier(planIdentifier);
  }

  @Operation(summary = "Create a plan",
      description = "Create a Plan",
      tags = {"Plan"}
  )
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(value = "/plan", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Plan createPlan(@Validated @RequestBody Plan plan) {
    return planService.createPlan(plan);
  }
}