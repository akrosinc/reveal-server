package com.revealprecision.revealserver.enums;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public enum GroupTypeEnum implements Serializable {
  FAMILY("family");


  private final String value;
  GroupTypeEnum(String value){
    this.value =  value;
  }

  private static final Map<String, GroupTypeEnum> BY_VALUE = new HashMap<>();

  static {
    for (GroupTypeEnum e: values()) {
      BY_VALUE.put(e.value, e);
    }
  }

  public static GroupTypeEnum getEnum(String value){
    return BY_VALUE.get(value);
  }
}
