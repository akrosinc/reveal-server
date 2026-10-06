package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RasterStatisticsCalculationRequest {
  @NotNull
  private String rasterId;
  @NotNull
  private String tagName;
  private String valueType;
  private Boolean reprocess;
  private List<String> skipGeographicLevels;
  public boolean isReprocess() {
    return Boolean.TRUE.equals(reprocess);
  }
}
