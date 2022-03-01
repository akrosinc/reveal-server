package com.revealprecision.revealserver.enums;

import java.util.HashMap;
import java.util.Map;

public enum ActionTypeEnum {
  CREATE("CREATE"),
  UPDATE("UPDATE"),
  REMOVE("REMOVE");

  final private String val;

  ActionTypeEnum(String val){
    this.val = val;
  }

  private static final Map<String, ActionTypeEnum> BY_VALUE = new HashMap<>();

  static {
    for (ActionTypeEnum e : values()) {
      BY_VALUE.put(e.val, e);
    }
  }

  public static ActionTypeEnum getEnum(String value) {
    ActionTypeEnum actionTypeEnum = BY_VALUE.get(value);
    if (actionTypeEnum == null) throw new IllegalArgumentException("No enum matching string: "+value);
    return actionTypeEnum;
  }

}
