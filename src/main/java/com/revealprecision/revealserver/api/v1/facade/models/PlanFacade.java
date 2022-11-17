package com.revealprecision.revealserver.api.v1.facade.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.revealprecision.revealserver.api.v1.dto.request.EffectivePeriod;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import java.time.LocalDate;
import java.util.List;
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
public class PlanFacade {

  private String identifier;
  private String description;
  private String version;
  private String name;
  private String title;
  private PlanStatusEnum status;
  private LocalDate date;
  private EffectivePeriod effectivePeriod;
  private List<UseContext> useContext;
  private List<JurisdictionFacade> jurisdiction;
  private Long serverVersion;
  private List<GoalFacade> goals;
  private List<ActionFacade> actions;
  private boolean experimental;
  private String targetGeographicLevel;
  private List<String> hierarchyGeographicLevels;
}