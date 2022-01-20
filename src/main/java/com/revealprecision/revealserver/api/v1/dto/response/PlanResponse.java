package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.revealprecision.revealserver.api.v1.dto.request.EffectivePeriod;
import com.revealprecision.revealserver.enums.PlanInterventionTypeEnum;
import java.util.Set;
import java.util.UUID;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanResponse {

  private UUID identifier;
  private String name;
  private String title;
  private EffectivePeriod effectivePeriod;
  private PlanInterventionTypeEnum interventionType;
  private Set<GoalResponse> goals;
  private Set<ActionResponse> actions;

}
