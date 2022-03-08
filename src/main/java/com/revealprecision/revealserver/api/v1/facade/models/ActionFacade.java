package com.revealprecision.revealserver.api.v1.facade.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.revealprecision.revealserver.api.v1.dto.request.EffectivePeriod;
import com.revealprecision.revealserver.api.v1.dto.request.SubjectCodableConcept;
import com.revealprecision.revealserver.enums.ActionTypeEnum;
import java.util.Set;
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
public class ActionFacade {

  private String identifier;
  private int prefix;
  private String title;
  private String description;
  private String code;
  private EffectivePeriod timingPeriod;
  private String reason;
  private String goalId;
  private SubjectCodableConcept subjectCodableConcept;
  private String taskTemplate;
  private Set<TriggerFacade> trigger;
  private Set<ConditionFacade> condition;
  private String definitionUri;
  private Set<DynamicValueFacade> dynamicValue;
  private ActionTypeEnum type;
}