package com.revealprecision.revealserver.util;

import static com.revealprecision.revealserver.constants.EntityTagDataTypes.BOOLEAN;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.INTEGER;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.STRING;

import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EntityTagEventUtil {

  public static Object getEntityTagEventValue(EntityTagEvent entityTagEvent, Object value) {

    switch (entityTagEvent.getValueType()) {
      case INTEGER:
        int i = (value instanceof Integer) ? (Integer) value : Integer.parseInt((String) value);
        log.trace("get_ request for {} = {}", entityTagEvent.getTag(), i);
        return i;
      case STRING:
        String s = String.valueOf(value);
        log.trace("get_ request for {} = {}", entityTagEvent.getTag(), s);
        return s;
      case BOOLEAN:
        boolean boolVal = (boolean) value;
        log.trace("get_ request for {} = {}", entityTagEvent.getTag(), boolVal);
        return boolVal;
      default:
        return value;
    }
  }

}
