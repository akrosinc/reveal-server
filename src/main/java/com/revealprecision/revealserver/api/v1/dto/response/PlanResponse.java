package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.revealprecision.revealserver.api.v1.dto.request.EffectivePeriod;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import com.revealprecision.revealserver.persistence.domain.PlanTargetType;
import java.time.LocalDate;
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
  private PlanStatusEnum status;
  private LocalDate date;
  private EffectivePeriod effectivePeriod;
  private LocationHierarchyResponse locationHierarchy;
  private LookupInterventionTypeResponse interventionType;
  private Set<GoalResponse> goals;
  private String planTargetType;

}
