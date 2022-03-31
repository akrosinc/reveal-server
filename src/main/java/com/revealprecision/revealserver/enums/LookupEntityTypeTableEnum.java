package com.revealprecision.revealserver.enums;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LookupEntityTypeTableEnum implements Serializable {
  PERSON_TABLE("person"), LOCATION_TABLE("location"), GROUP_TABLE("group");

  private final String lookupEntityType;

}
