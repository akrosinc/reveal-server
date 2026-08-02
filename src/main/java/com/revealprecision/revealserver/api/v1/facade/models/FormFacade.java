package com.revealprecision.revealserver.api.v1.facade.models;

import javax.validation.constraints.NotBlank;
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
public class FormFacade {

  @NotBlank
  private String name;
  @NotBlank
  private String template;
}
