package com.revealprecision.revealserver.persistence.projection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.revealprecision.revealserver.api.v1.dto.request.EffectivePeriod;
import com.revealprecision.revealserver.api.v1.dto.response.LookupInterventionTypeResponse;
import java.time.LocalDate;
import java.util.UUID;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstancePlanProjection {
  private UUID identifier;
  private String instanceName;
  private String planTitle;
  private LocalDate date;
  private EffectivePeriod effectivePeriod;
  private LookupInterventionTypeResponse interventionType;
}
