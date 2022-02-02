package com.revealprecision.revealserver.api.v1.dto.request;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Context {

  @NotBlank
  private String code;
  @NotBlank
  private String valueCodableConcept;
}
