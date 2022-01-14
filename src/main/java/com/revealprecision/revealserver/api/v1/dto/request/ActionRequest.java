package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.ActionTypeEnum;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActionRequest {

  @NotBlank
  private String title;

  @NotBlank
  private String description;

  @NotBlank
  private String code;

  @Valid
  private EffectivePeriod timingPeriod;

  @NotBlank
  private String reason;

  @NotBlank
  private String definitionUri;

  @NotBlank
  private String goalId;

  @NotNull
  private ActionTypeEnum type;

  @Valid
  private SubjectCodableConcept subjectCodableConcept;
}
