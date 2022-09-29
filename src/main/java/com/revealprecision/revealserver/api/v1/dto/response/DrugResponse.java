package com.revealprecision.revealserver.api.v1.dto.response;

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
public class DrugResponse {

  private String name;
  private Object min;
  private Object max;
  private boolean half;
  private boolean full;
  private boolean millis;
  private String key;
}
