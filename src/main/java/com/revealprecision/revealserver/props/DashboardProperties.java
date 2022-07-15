package com.revealprecision.revealserver.props;

import static com.revealprecision.revealserver.service.dashboard.DashboardService.ALL_OTHER_LEVELS;
import static com.revealprecision.revealserver.service.dashboard.DashboardService.DIRECTLY_ABOVE_STRUCTURE_LEVEL;
import static com.revealprecision.revealserver.service.dashboard.IRSDashboardService.SPRAY_COVERAGE_OF_TARGETED;
import static com.revealprecision.revealserver.service.dashboard.MDADashboardService.DISTRIBUTION_COVERAGE;
import static com.revealprecision.revealserver.service.dashboard.MDADashboardService.DISTRIBUTION_COVERAGE_PERCENTAGE;
import static com.revealprecision.revealserver.service.dashboard.MDALiteDashboardService.ALB;
import static com.revealprecision.revealserver.service.dashboard.MDALiteDashboardService.DRUG;
import static com.revealprecision.revealserver.service.dashboard.MDALiteDashboardService.MALES_1_4;
import static com.revealprecision.revealserver.service.dashboard.MDALiteDashboardService.MBZ;
import static com.revealprecision.revealserver.service.dashboard.MDALiteDashboardService.PZQ;

import com.revealprecision.revealserver.enums.ReportTypeEnum;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "dashboard")
@Component
@Setter
@Getter
public class DashboardProperties {


  private List<String> dashboards = List.of("IRS_COVERAGE","MDA_COVERAGE");

  private Map<String,Map<String,ColumnMeta>> dashboardColumns = Map.of("MDA_COVERAGE",
      Map.of("TOTAL_STRUCTURES",new ColumnMeta("Total Structures",false)
          ,"TOTAL_STRUCTURES_FOUND",new ColumnMeta("Total Structures Found",false)
          ,"FOUND_COVERAGE",new ColumnMeta("Found Coverage",true)
          ,"OPERATIONAL_AREA_VISITED",new ColumnMeta("Operational Area Visited",false)));

  private final Map<String, String> mdaDefaultDisplayColumns =
      Map.of(DIRECTLY_ABOVE_STRUCTURE_LEVEL,DISTRIBUTION_COVERAGE_PERCENTAGE,
          ALL_OTHER_LEVELS,DISTRIBUTION_COVERAGE);

  private final Map<String, String> irsDefaultDisplayColumns =
      Map.of(DIRECTLY_ABOVE_STRUCTURE_LEVEL,SPRAY_COVERAGE_OF_TARGETED,
          ALL_OTHER_LEVELS,SPRAY_COVERAGE_OF_TARGETED);

  private final Map<String, String> mdaLiteDefaultDisplayColumns =
      Map.of(DIRECTLY_ABOVE_STRUCTURE_LEVEL,MALES_1_4,
          ALL_OTHER_LEVELS,MALES_1_4);


  private Long operationalAreaVisitedThreshold = 20L;
  private Long operationalAreaVisitedEffectivelyThreshold = 85L;

  private final Map<String, List<String>> mdaLiteFilters = Map.of(
      DRUG, List.of(ALB,MBZ,PZQ)
  );

  private final Map<ReportTypeEnum, Map<String, List<String>>> dashboardFilterAssociations = Map.of(
      ReportTypeEnum.MDA_LITE_COVERAGE,mdaLiteFilters
  );


  @Setter
  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class ColumnMeta{
    private String name;
    private Boolean isPercentage;
  }
}
