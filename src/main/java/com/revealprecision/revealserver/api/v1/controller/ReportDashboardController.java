package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.models.TableRow;
import com.revealprecision.revealserver.api.v1.dto.request.TableReportRequest;
import com.revealprecision.revealserver.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/dashboard")
public class ReportDashboardController {

  private final DashboardService dashboardService;

  @GetMapping("/row")
  public TableRow getRow(TableReportRequest tableReportRequest) {

    return dashboardService.getRowData(tableReportRequest.getPlanIdentifier(),
        tableReportRequest.getParentLocationIdentifier(), tableReportRequest.getReportIdentifier(), tableReportRequest.getGetChildren());
  }
}
