package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.ActionTypeEnum;
import java.util.UUID;
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

  @Valid
  private EffectivePeriod timingPeriod;

  @NotBlank
  private String reason;

  @NotNull
  private UUID formIdentifier;

  @NotNull
  private ActionTypeEnum type;

}
