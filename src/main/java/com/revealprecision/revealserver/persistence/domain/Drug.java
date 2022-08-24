package com.revealprecision.revealserver.persistence.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Drug {

  private String name;
  private int min;
  private int max;
  private boolean half;
  private boolean full;
  private boolean millis;
}
