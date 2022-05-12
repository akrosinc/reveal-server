package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.models.TableRow;
import com.revealprecision.revealserver.api.v1.dto.request.TableReportRequest;
import com.revealprecision.revealserver.enums.ApplicableReportsEnum;
import com.revealprecision.revealserver.enums.ReportTypeEnum;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.service.DashboardService;
import com.revealprecision.revealserver.service.PlanService;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.PathParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/dashboard")
public class ReportDashboardController {

  private final DashboardService dashboardService;

  private final PlanService planService;

  @GetMapping("/row")
  public TableRow getRow(TableReportRequest tableReportRequest) {

    return dashboardService.getRowData(tableReportRequest.getPlanIdentifier(),
        tableReportRequest.getParentLocationIdentifier(), tableReportRequest.getGetChildren(), tableReportRequest.getReportTypeEnum());
  }

  @GetMapping("/reportTypes")
  public ReportTypeEnum[] getReportTypes() {
    return ReportTypeEnum.values();
  }

  @GetMapping("/applicableReports/{identifier}")
  public List<String> getReportTypes(@PathVariable("identifier") UUID identifier) {
    Plan plan = planService.getPlanByIdentifier(identifier);
    return ApplicableReportsEnum.valueOf(plan.getInterventionType().getCode()).getReportName();
  }

}
