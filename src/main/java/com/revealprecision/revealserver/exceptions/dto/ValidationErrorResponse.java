package com.revealprecision.revealserver.exceptions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ValidationErrorResponse {

  private String field;
  private String rejectedValue;
  private String messageKey;
}
