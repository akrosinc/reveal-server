package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.Map;
import java.util.UUID;
import javax.validation.constraints.NotBlank;
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
public class RasterIngestionRequest {
  @NotBlank
  private String rasterId;
  @NotBlank
  private String name;
  @NotNull
  private Integer minZoom;
  @NotNull
  private Integer maxZoom;
  private Map<String, Object> metadata;
  @NotBlank
  private String layerName;
  @NotBlank
  private String valueAttribute;
  private boolean convertToCog;
  private RasterStatisticsCalculationRequest statisticsCalculation;
}
