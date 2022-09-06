package com.revealprecision.revealserver.util;

import com.revealprecision.revealserver.api.v1.facade.models.Obs;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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

  public static Map<String, Object> extractDataFromList(Obs obs) { //TODO:
    Map<String, Object> response = new HashMap<>();
    if (obs.getKeyValPairs() == null) {
      response.put(obs.getFieldCode(),
          obs.getValues().stream().map(String::valueOf).collect(Collectors.joining("|")));
    } else {
      response.put(obs.getFieldCode(), obs.getKeyValPairs().get(obs.getValues().get(0)));
    }
    response.computeIfAbsent(obs.getFieldCode(), k -> obs.getValues().get(0));
    return response;
  }

}
