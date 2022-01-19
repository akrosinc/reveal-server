package com.revealprecision.revealserver.enums;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/*
Genders are introduced as defined by https://www.hl7.org/fhir/person.html
*/
@Getter
public enum GenderEnum {
  MALE("male"),
  FEMALE("female"),
  OTHER("other"),
  UNKNOWN("unknown");

  private final String value;

  GenderEnum(String value) {
    this.value = value;
  }

  private static final Map<String, GenderEnum> BY_VALUE = new HashMap<>();

  static {
    for (GenderEnum e : values()) {
      BY_VALUE.put(e.value, e);
    }
  }

  public static GenderEnum getEnum(String value) {
    return BY_VALUE.get(value);
  }

}



