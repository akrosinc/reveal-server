package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.revealprecision.revealserver.api.v1.dto.request.EffectivePeriod;
import com.revealprecision.revealserver.enums.ActionTypeEnum;
import com.revealprecision.revealserver.enums.EntityPropertiesEnum;
import java.util.Set;
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
public class ActionResponse {

  private UUID identifier;
  private String title;
  private String description;
  private EffectivePeriod timingPeriod;
  private EntityPropertiesEnum subject;
  private ActionTypeEnum type;
  private Set<ConditionResponse> conditions;
  private UUID formIdentifier;
}
