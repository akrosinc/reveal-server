package com.revealprecision.revealserver.enums;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrganizationTypeEnum implements Serializable {
  CG("Community group"),
  TEAM("Team"),
  OTHER("Other");

  private String organizationType;

}
