package com.revealprecision.revealserver.enums;

import com.revealprecision.revealserver.exceptions.WrongEnumException;
import com.revealprecision.revealserver.exceptions.constant.Error;

public class LookupUtil {

  public static <E extends Enum<E>> E lookup(Class<E> e, String id) {
    E result;
    try {
      result = Enum.valueOf(e, id.toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new WrongEnumException(String.format(Error.WRONG_ENUM, e.getSimpleName(), id));
    }

    return result;
  }
}
