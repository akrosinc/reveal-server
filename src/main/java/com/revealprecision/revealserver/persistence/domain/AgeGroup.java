package com.revealprecision.revealserver.persistence.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgeGroup {

  private String name;
  private int min;
  private int max;
  private String key;
}
