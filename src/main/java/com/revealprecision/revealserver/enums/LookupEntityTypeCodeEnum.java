package com.revealprecision.revealserver.enums;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LookupEntityTypeCodeEnum implements Serializable {
  PERSON_CODE("Person"), LOCATION_CODE("Location"), GROUP("Group");

  private final String lookupEntityType;

}
