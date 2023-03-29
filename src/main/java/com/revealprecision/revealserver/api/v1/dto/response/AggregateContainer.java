package com.revealprecision.revealserver.api.v1.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AggregateContainer {

  Double sum;
  Double cnt;
  Double avg;
}
