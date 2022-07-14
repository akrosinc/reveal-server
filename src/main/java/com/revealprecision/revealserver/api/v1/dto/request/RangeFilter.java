package com.revealprecision.revealserver.api.v1.dto.request;

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
public class RangeFilter {

  private Object minValue;
  private Object maxValue;
}
