package com.revealprecision.revealserver.util;

import com.revealprecision.revealserver.api.v1.facade.models.Obs;
import java.util.HashMap;
import java.util.Map;

public class FormDataUtil {
  public static Map<String, Object> extractData(Obs obs) { //TODO:
    Map<String, Object> response = new HashMap<>();
    if (obs.getKeyValPairs() == null) {
      response.put(obs.getFieldCode(), obs.getValues().get(0));
    } else {
      response.put(obs.getFieldCode(), obs.getKeyValPairs().get(obs.getValues().get(0)));
    }
    response.computeIfAbsent(obs.getFieldCode(), k -> obs.getValues().get(0));
    return response;
  }
}
