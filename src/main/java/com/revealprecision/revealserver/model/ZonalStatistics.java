package com.revealprecision.revealserver.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZonalStatistics {

  private long pixelCount;
  @Builder.Default
  private double min = Double.MAX_VALUE;
  @Builder.Default
  private double max = -Double.MAX_VALUE;
  private double sum;
  private double mean;
}
