package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.ActionResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.GoalResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.ActionRequest;
import com.revealprecision.revealserver.api.v1.dto.request.GoalRequest;
import com.revealprecision.revealserver.api.v1.dto.request.GoalUpdateRequest;
import com.revealprecision.revealserver.api.v1.dto.request.PlanRequest;
import com.revealprecision.revealserver.api.v1.dto.response.ActionResponse;
import com.revealprecision.revealserver.api.v1.dto.response.GoalResponse;
import com.revealprecision.revealserver.service.ActionService;
import com.revealprecision.revealserver.service.GoalService;
import com.revealprecision.revealserver.service.PlanService;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.UUID;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/plan")
public class PlanController {

  private final PlanService planService;
  private final GoalService goalService;
  private final ActionService actionService;

  @PostMapping
  public ResponseEntity<Void> createPlan(@Valid @RequestBody PlanRequest planRequest) {
    planService.createPlan(planRequest);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @GetMapping("/{identifier}/goal")
  public ResponseEntity<Page<GoalResponse>> getGoals(
      @Parameter(name = "Plan identifier") @PathVariable("identifier") UUID identifier,
      Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK).body(
        GoalResponseFactory.fromEntityPage(goalService.getGoals(identifier, pageable), pageable));
  }

  @PostMapping("/{identifier}/goal")
  public ResponseEntity<Void> createGoal(
      @Parameter(name = "Plan identifier") @PathVariable("identifier") UUID identifier,
      @Valid @RequestBody GoalRequest goalRequest) {
    goalService.createGoal(identifier, goalRequest);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PutMapping("/{planIdentifier}/goal/{identifier}")
  public ResponseEntity<Void> updateGoal(
      @Parameter(name = "Plan identifier") @PathVariable("planIdentifier") UUID planIdentifier,
      @Parameter(name = "Goal identifier") @PathVariable("identifier") String identifier,
      @Valid @RequestBody GoalUpdateRequest goalUpdateRequest) {
    goalService.updateGoal(identifier, planIdentifier, goalUpdateRequest);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @GetMapping("/{planIdentifier}/goal/{goalIdentifier}/action")
  public ResponseEntity<Page<ActionResponse>> getActions(
      @Parameter(name = "Plan identifier") @PathVariable("planIdentifier") UUID planIdentifier,
      @Parameter(name = "Goal identifier") @PathVariable("goalIdentifier") String goalIdentifier,
      Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK).body(ActionResponseFactory.fromEntityPage(
        actionService.getActions(planIdentifier, goalIdentifier, pageable), pageable));
  }

  @PostMapping("/{planIdentifier}/goal/{goalIdentifier}/action")
  public ResponseEntity<Void> createAction(
      @Parameter(name = "Plan identifier") @PathVariable("planIdentifier") UUID planIdentifier,
      @Parameter(name = "Goal identifier") @PathVariable("goalIdentifier") String goalIdentifier,
      @Valid @RequestBody ActionRequest actionRequest) {
    actionService.createAction(planIdentifier, goalIdentifier, actionRequest);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PutMapping("/{planIdentifier}/goal/{goalIdentifier}/action/{actionIdentifier}")
  public ResponseEntity<Void> updateAction(
      @Parameter(name = "Plan identifier") @PathVariable("planIdentifier") UUID planIdentifier,
      @Parameter(name = "Goal identifier") @PathVariable("goalIdentifier") String goalIdentifier,
      @Parameter(name = "Action identifier") @PathVariable("actionIdentifier") UUID actionIdentifier,
      @Valid @RequestBody ActionRequest actionRequest) {
    actionService.updateAction(planIdentifier, goalIdentifier, actionRequest, actionIdentifier);
    return ResponseEntity.status(HttpStatus.OK).build();
  }
}