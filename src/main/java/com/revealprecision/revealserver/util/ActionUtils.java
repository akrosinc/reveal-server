package com.revealprecision.revealserver.util;

import com.revealprecision.revealserver.enums.LookupEntityTypeCodeEnum;
import com.revealprecision.revealserver.persistence.domain.Action;

public class ActionUtils {

  public static Boolean isActionForPerson(Action action) {
    return action.getLookupEntityType() != null && LookupEntityTypeCodeEnum.PERSON_CODE
        .getLookupEntityType().equals(
            action.getLookupEntityType().getCode());
  }

  public static Boolean isActionForLocation(Action action) {
    return action.getLookupEntityType() != null && LookupEntityTypeCodeEnum.LOCATION_CODE
        .getLookupEntityType().equals(action.getLookupEntityType().getCode());
  }

  public static Boolean isActionForGroup(Action action) {
    return action.getLookupEntityType() != null && LookupEntityTypeCodeEnum.GROUP_CODE
        .getLookupEntityType().equals(action.getLookupEntityType().getCode());
  }
}
