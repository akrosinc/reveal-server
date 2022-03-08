package com.revealprecision.revealserver.api.v1.facade.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TriggerFacade {

  private String type;
  private String name;
  private ExpressionFacade expression;
  private TimingFacade timingTiming;
}