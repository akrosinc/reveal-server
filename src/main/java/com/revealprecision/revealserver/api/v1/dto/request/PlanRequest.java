package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.Set;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
public class PlanRequest {

  @NotBlank
  private String name;

  @NotBlank
  private String title;

  @NotNull
  @Valid
  private EffectivePeriod effectivePeriod;

  @NotNull
  private UUID interventionType;

  @NotNull
  private UUID locationHierarchy;

  @Valid
  private Set<GoalRequest> goals;

  private String hierarchyLevelTarget;
}
