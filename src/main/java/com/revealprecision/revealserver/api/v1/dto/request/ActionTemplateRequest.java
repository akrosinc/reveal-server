package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.model.ActionTemplateConfig;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionTemplateRequest {

  @NotBlank
  private String name;

  private ActionTemplateConfig actionTemplateConfig;
}
