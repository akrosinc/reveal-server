package com.revealprecision.revealserver.api.v1.dto.response;

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
public class GroupStatsResponse {
  private Long targetAreas;
  private Long totalStructures;
  private Long totalPopulation;
  private Double completionPercentage;
}
