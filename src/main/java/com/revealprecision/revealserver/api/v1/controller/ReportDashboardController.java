package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.models.AdditionalReportInfo;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.enums.ApplicableReportsEnum;
import com.revealprecision.revealserver.enums.ReportTypeEnum;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.props.DashboardProperties;
import com.revealprecision.revealserver.service.PlanService;
import com.revealprecision.revealserver.service.dashboard.DashboardService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/dashboard")
public class ReportDashboardController {

  private final DashboardService dashboardService;
  private final PlanService planService;
  private final DashboardProperties dashboardProperties;

  @GetMapping("/reportTypes")
  public ReportTypeEnum[] getReportTypes() {
    return ReportTypeEnum.values();
  }

  @GetMapping("/applicableReports/{identifier}")
  public List<String> getReportTypes(@PathVariable("identifier") UUID identifier) {
    Plan plan = planService.findPlanByIdentifier(identifier);
    return ApplicableReportsEnum.valueOf(plan.getInterventionType().getCode()).getReportName();
  }

  @GetMapping("/reportData")
  public ResponseEntity<FeatureSetResponse> getDataForReports(
      @RequestParam(name = "reportType") String reportType,
      @RequestParam(name = "planIdentifier") UUID planIdentifier,
      @RequestParam(name = "parentIdentifier", required = false) UUID parentIdentifier,
      @RequestParam(name = "filters", required = false) List<String> filters) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(dashboardService.getDataForReport(reportType, planIdentifier, parentIdentifier,
            filters));
  }

  @GetMapping("/reportAdditionalInfo")
  public ResponseEntity<AdditionalReportInfo> getDataForReports(
      @RequestParam(name = "reportType") String reportType) {
    Map<String, List<String>> dashboardFilter = dashboardProperties.getDashboardFilterAssociations()
        .get(ReportTypeEnum.valueOf(reportType));
    return ResponseEntity.status(HttpStatus.OK)
        .body(AdditionalReportInfo.builder()
            .dashboardFilter(dashboardFilter)
            .reportTypeEnum(ReportTypeEnum.valueOf(reportType))
            .build()
        );
  }

}
