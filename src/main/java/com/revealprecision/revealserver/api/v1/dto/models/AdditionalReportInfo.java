package com.revealprecision.revealserver.api.v1.dto.models;

import com.revealprecision.revealserver.enums.ReportTypeEnum;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdditionalReportInfo implements Serializable {
  private ReportTypeEnum reportTypeEnum;
  private Map<String, List<String>> dashboardFilter;
}
