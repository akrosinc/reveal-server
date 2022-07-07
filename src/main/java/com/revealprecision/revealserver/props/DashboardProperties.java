package com.revealprecision.revealserver.props;

import static com.revealprecision.revealserver.service.dashboard.DashboardService.ALL_OTHER_LEVELS;
import static com.revealprecision.revealserver.service.dashboard.DashboardService.DIRECTLY_ABOVE_STRUCTURE_LEVEL;
import static com.revealprecision.revealserver.service.dashboard.IRSDashboardService.SPRAY_COVERAGE_OF_TARGETED;
import static com.revealprecision.revealserver.service.dashboard.MDADashboardService.DISTRIBUTION_COVERAGE;

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
      Map.of(DIRECTLY_ABOVE_STRUCTURE_LEVEL,DISTRIBUTION_COVERAGE,
          ALL_OTHER_LEVELS,DISTRIBUTION_COVERAGE);

  private final Map<String, String> irsDefaultDisplayColumns =
      Map.of(DIRECTLY_ABOVE_STRUCTURE_LEVEL,SPRAY_COVERAGE_OF_TARGETED,
          ALL_OTHER_LEVELS,SPRAY_COVERAGE_OF_TARGETED);


  private Long operationalAreaVisitedThreshold = 20L;
  private Long operationalAreaVisitedEffectivelyThreshold = 85L;

  @Setter
  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class ColumnMeta{
    private String name;
    private Boolean isPercentage;
  }
}
