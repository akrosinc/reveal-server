package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.ActionResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.ConditionResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.GoalResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.PlanResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.TargetResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.ActionRequest;
import com.revealprecision.revealserver.api.v1.dto.request.ConditionRequest;
import com.revealprecision.revealserver.api.v1.dto.request.GoalRequest;
import com.revealprecision.revealserver.api.v1.dto.request.GoalUpdateRequest;
import com.revealprecision.revealserver.api.v1.dto.request.PlanRequest;
import com.revealprecision.revealserver.api.v1.dto.request.TargetRequest;
import com.revealprecision.revealserver.api.v1.dto.response.ActionResponse;
import com.revealprecision.revealserver.api.v1.dto.response.ConditionResponse;
import com.revealprecision.revealserver.api.v1.dto.response.CountResponse;
import com.revealprecision.revealserver.api.v1.dto.response.GoalResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PlanResponse;
import com.revealprecision.revealserver.api.v1.dto.response.TargetResponse;
import com.revealprecision.revealserver.enums.SummaryEnum;
import com.revealprecision.revealserver.service.ActionService;
import com.revealprecision.revealserver.service.ConditionService;
import com.revealprecision.revealserver.service.GoalService;
import com.revealprecision.revealserver.service.PlanService;
import com.revealprecision.revealserver.service.TargetService;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.UUID;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/plan")
public class PlanController {

  private final PlanService planService;
  private final GoalService goalService;
  private final ActionService actionService;
  private final TargetService targetService;
  private final ConditionService conditionService;

  @GetMapping
  public ResponseEntity<?> getPlans(
      @Parameter(description = "Search parameter") @RequestParam(name = "search", defaultValue = "") String search,
      @Parameter(description = "Toggle summary data") @RequestParam(name = "_summary", defaultValue = "TRUE", required = false) SummaryEnum summary,
      Pageable pageable) {
    if (!summary.equals(SummaryEnum.COUNT)) {
      return ResponseEntity.status(HttpStatus.OK)
          .body(PlanResponseFactory.fromEntityPage(planService.getAll(search, pageable),
              pageable,
              summary));
    } else {
      return ResponseEntity.status(HttpStatus.OK)
          .body(new CountResponse(planService.getAllCount(search)));
    }
  }

  @GetMapping("/{identifier}")
  public ResponseEntity<PlanResponse> getByIdentifier(@PathVariable("identifier") UUID identifier) {
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(PlanResponseFactory.fromEntity(planService.getPlanByIdentifier(identifier)));
  }

  @PostMapping
  public ResponseEntity<Void> createPlan(@Valid @RequestBody PlanRequest planRequest) {
    planService.createPlan(planRequest);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PutMapping("/{identifier}")
  public ResponseEntity<Void> updatePlan(@PathVariable("identifier") UUID identifier,
      @RequestBody PlanRequest request) {
    planService.updatePlan(request, identifier);
    return ResponseEntity.status(HttpStatus.OK).build();
  }


  @GetMapping("/{identifier}/goal") //done
  public ResponseEntity<Page<GoalResponse>> getGoals(
      @Parameter(description = "Plan identifier") @PathVariable("identifier") UUID identifier,
      Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK).body(
        GoalResponseFactory.fromEntityPage(goalService.getGoals(identifier, pageable), pageable));
  }

  @PostMapping("/{identifier}/goal")
  public ResponseEntity<Void> createGoal(
      @Parameter(description = "Plan identifier") @PathVariable("identifier") UUID identifier,
      @Valid @RequestBody GoalRequest goalRequest) {
    goalService.createGoal(identifier, goalRequest);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PutMapping("/{planIdentifier}/goal/{identifier}")
  public ResponseEntity<Void> updateGoal(
      @Parameter(description = "Plan identifier") @PathVariable("planIdentifier") UUID planIdentifier,
      @Parameter(description = "Goal identifier") @PathVariable("identifier") UUID identifier,
      @Valid @RequestBody GoalUpdateRequest goalUpdateRequest) {
    goalService.updateGoal(identifier, planIdentifier, goalUpdateRequest);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @DeleteMapping("/{planIdentifier}/goal/{identifier}")
  public ResponseEntity<Void> deleteGoal(
      @Parameter(description = "Plan identifier") @PathVariable("planIdentifier") UUID planIdentifier,
      @Parameter(description = "Goal identifier") @PathVariable("identifier") UUID identifier) {
    goalService.deleteGoal(identifier, planIdentifier);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @GetMapping("/{planIdentifier}/goal/{goalIdentifier}/action")//done
  public ResponseEntity<Page<ActionResponse>> getActions(
      @Parameter(description = "Plan identifier") @PathVariable("planIdentifier") UUID planIdentifier,
      @Parameter(description = "Goal identifier") @PathVariable("goalIdentifier") UUID goalIdentifier,
      Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK).body(ActionResponseFactory.fromEntityPage(
        actionService.getActions(planIdentifier, goalIdentifier, pageable), pageable));
  }

  @PostMapping("/{planIdentifier}/goal/{goalIdentifier}/action")
  public ResponseEntity<Void> createAction(
      @Parameter(description = "Plan identifier") @PathVariable("planIdentifier") UUID planIdentifier,
      @Parameter(description = "Goal identifier") @PathVariable("goalIdentifier") UUID goalIdentifier,
      @Valid @RequestBody ActionRequest actionRequest) {
    actionService.createAction(planIdentifier, goalIdentifier, actionRequest);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PutMapping("/{planIdentifier}/goal/{goalIdentifier}/action/{actionIdentifier}")
  public ResponseEntity<Void> updateAction(
      @Parameter(description = "Plan identifier") @PathVariable("planIdentifier") UUID planIdentifier,
      @Parameter(description = "Goal identifier") @PathVariable("goalIdentifier") UUID goalIdentifier,
      @Parameter(description = "Action identifier") @PathVariable("actionIdentifier") UUID actionIdentifier,
      @Valid @RequestBody ActionRequest actionRequest) {
    actionService.updateAction(planIdentifier, goalIdentifier, actionRequest, actionIdentifier);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @DeleteMapping("/{planIdentifier}/goal/{goalIdentifier}/action/{actionIdentifier}")
  public ResponseEntity<Void> deleteAction(
      @Parameter(description = "Plan identifier") @PathVariable("planIdentifier") UUID planIdentifier,
      @Parameter(description = "Goal identifier") @PathVariable("goalIdentifier") UUID goalIdentifier,
      @Parameter(description = "Action identifier") @PathVariable("actionIdentifier") UUID actionIdentifier) {
    actionService.deleteAction(planIdentifier, goalIdentifier, actionIdentifier);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @GetMapping("/{planIdentifier}/goal/{goalIdentifier}/action/{actionIdentifier}/condition")//done
  public ResponseEntity<Page<ConditionResponse>> getConditions(
      @Parameter(description = "Plan identifier") @PathVariable("planIdentifier") UUID planIdentifier,
      @Parameter(description = "Goal identifier") @PathVariable("goalIdentifier") UUID goalIdentifier,
      @Parameter(description = "Action identifier") @PathVariable("actionIdentifier") UUID actionIdentifier,
      Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ConditionResponseFactory.fromEntityPage(
            conditionService.getConditions(planIdentifier, goalIdentifier, actionIdentifier,
                pageable), pageable));
  }

  @PostMapping("/{planIdentifier}/goal/{goalIdentifier}/action/{actionIdentifier}/condition")
  public ResponseEntity<Void> createCondition(
      @Parameter(description = "Plan identifier") @PathVariable("planIdentifier") UUID planIdentifier,
      @Parameter(description = "Goal identifier") @PathVariable("goalIdentifier") UUID goalIdentifier,
      @Parameter(description = "Action identifier") @PathVariable("actionIdentifier") UUID actionIdentifier,
      @RequestBody ConditionRequest request) {
    conditionService.createCondition(planIdentifier, goalIdentifier, actionIdentifier, request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @DeleteMapping("/{planIdentifier}/goal/{goalIdentifier}/action/{actionIdentifier}/condition/{conditionIdentifier}")
  public ResponseEntity<Void> deleteCondition(
      @Parameter(description = "Plan identifier") @PathVariable("planIdentifier") UUID planIdentifier,
      @Parameter(description = "Goal identifier") @PathVariable("goalIdentifier") UUID goalIdentifier,
      @Parameter(description = "Action identifier") @PathVariable("actionIdentifier") UUID actionIdentifier,
      @Parameter(description = "Condition identifier") @PathVariable("conditionIdentifier") UUID conditionIdentifier) {
    conditionService.deleteCondition(planIdentifier, goalIdentifier, actionIdentifier,
        conditionIdentifier);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @GetMapping("/{planIdentifier}/goal/{goalIdentifier}/action/{actionIdentifier}/condition/{conditionIdentifier}/target")
  public ResponseEntity<Page<TargetResponse>> getTargets(
      @Parameter(description = "Plan identifier") @PathVariable("planIdentifier") UUID planIdentifier,
      @Parameter(description = "Goal identifier") @PathVariable("goalIdentifier") UUID goalIdentifier,
      @Parameter(description = "Action identifier") @PathVariable("actionIdentifier") UUID actionIdentifier,
      @Parameter(description = "Condition identifier") @PathVariable("conditionIdentifier") UUID conditionIdentifier,
      Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK).body(TargetResponseFactory.fromEntityPage(
        targetService.getAll(planIdentifier, goalIdentifier, actionIdentifier, conditionIdentifier,
            pageable), pageable));
  }

  @PostMapping("/{planIdentifier}/goal/{goalIdentifier}/target")
  public ResponseEntity<Void> createTarget(
      @Parameter(description = "Plan identifier") @PathVariable("planIdentifier") UUID planIdentifier,
      @Parameter(description = "Goal identifier") @PathVariable("goalIdentifier") UUID goalIdentifier,
      @Valid @RequestBody TargetRequest targetRequest) {
    targetService.createTarget(targetRequest, planIdentifier, goalIdentifier);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PutMapping("/{planIdentifier}/goal/{goalIdentifier}/target/{targetIdentifier}")
  public ResponseEntity<Void> updateTarget(
      @Parameter(description = "Plan identifier") @PathVariable("planIdentifier") UUID planIdentifier,
      @Parameter(description = "Goal identifier") @PathVariable("goalIdentifier") UUID goalIdentifier,
      @Parameter(description = "Target identifier") @PathVariable("targetIdentifier") UUID targetIdentifier,
      @Valid @RequestBody TargetRequest targetRequest) {
    targetService.updateTarget(targetRequest, planIdentifier, goalIdentifier, targetIdentifier);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @PatchMapping("/{planIdentifier}")
  public ResponseEntity<Void> activatePlan(
      @Parameter(description = "Plan identifier") @PathVariable("planIdentifier") UUID planIdentifier) {
    planService.activatePlan(planIdentifier);
    return ResponseEntity.status(HttpStatus.OK).build();
  }
}