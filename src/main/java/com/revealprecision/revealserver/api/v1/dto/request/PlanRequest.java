package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
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

  @NotEmpty
  @Valid
  private Set<Context> useContext;

  private List<Jurisdiction> jurisdictions;

  @Valid
  private Set<GoalRequest> goals;

  @Valid
  private Set<ActionRequest> actions;
}
