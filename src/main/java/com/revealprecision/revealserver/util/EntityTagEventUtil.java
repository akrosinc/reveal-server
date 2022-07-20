package com.revealprecision.revealserver.util;

import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import javax.swing.text.html.parser.Entity;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EntityTagEventUtil {

  public static Object getEntityTagEventValue(EntityTagEvent entityTagEvent, Object value) {

    switch (entityTagEvent.getValueType()) {
      case "integer":
        int i = (value instanceof Integer)? (Integer) value: Integer.parseInt((String) value);
        log.trace("get_ request for {} = {}", entityTagEvent.getTag(), i);
        return i;
      case "string":
        String s = String.valueOf(value);
        log.trace("get_ request for {} = {}", entityTagEvent.getTag(), s);
        return s;
      case "boolean":
        boolean boolVal = (boolean) value;
        log.trace("get_ request for {} = {}", entityTagEvent.getTag(), boolVal);
        return boolVal;
      default:
        return value;
    }
  }

}
