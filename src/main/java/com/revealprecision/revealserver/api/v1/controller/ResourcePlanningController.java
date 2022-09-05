package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.CampaignDrugResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.CountryCampaignFactory;
import com.revealprecision.revealserver.api.v1.dto.request.ResourcePlanningRequest;
import com.revealprecision.revealserver.api.v1.dto.response.CampaignDrugResponse;
import com.revealprecision.revealserver.api.v1.dto.response.CountryCampaignResponse;
import com.revealprecision.revealserver.api.v1.dto.response.FormulaResponse;
import com.revealprecision.revealserver.api.v1.dto.response.ResourcePlanningQuestionResponse;
import com.revealprecision.revealserver.service.ResourcePlanningService;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/resource-planning")
public class ResourcePlanningController {

  private final ResourcePlanningService resourcePlanningService;

  @GetMapping("country")
  public ResponseEntity<List<CountryCampaignResponse>> getCountries() {
    return ResponseEntity.ok()
        .body(resourcePlanningService.getCountries()
            .stream()
            .map(CountryCampaignFactory::fromEntity)
            .collect(Collectors.toList()));
  }

  @GetMapping("campaign")
  public ResponseEntity<List<CampaignDrugResponse>> getCampaigns() {
    return ResponseEntity.ok()
        .body(resourcePlanningService.getCampaigns()
            .stream()
            .map(CampaignDrugResponseFactory::fromEntity)
            .collect(Collectors.toList()));
  }

  @GetMapping("formula")
  public ResponseEntity<List<FormulaResponse>> getFormulas() {
    return ResponseEntity.ok()
        .body(Arrays.asList(FormulaResponse.list));
  }

  @PostMapping("formula")
  public ResponseEntity<ResourcePlanningQuestionResponse> getFormulasSecondStep(@Valid @RequestBody
      ResourcePlanningRequest request) {
    return ResponseEntity.ok()
        .body(resourcePlanningService.getSecondStepQuestions(request));
  }

}
