package com.revealprecision.revealserver.util;

import com.revealprecision.revealserver.enums.LookupEntityTypeCodeEnum;
import com.revealprecision.revealserver.persistence.domain.Action;

public  class ActionEntityUtil {
  public static boolean isEntityFor(Action action, LookupEntityTypeCodeEnum lookupEntityTypeCodeEnum) {
    return action.getLookupEntityType().getCode() != null
        && LookupEntityTypeCodeEnum.lookup(
        action.getLookupEntityType().getCode()).equals(lookupEntityTypeCodeEnum);
  }
}
