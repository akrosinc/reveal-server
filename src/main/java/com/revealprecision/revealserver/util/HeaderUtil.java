package com.revealprecision.revealserver.util;

import java.util.AbstractMap.SimpleEntry;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;

public class HeaderUtil {

  public static Map<String, String> getHeaderMapFromHttpServletRequest(
      HttpServletRequest httpServletRequest) {
    Map<String, String> requestHeaders = new HashMap<>();

    for (Enumeration<?> e = httpServletRequest.getHeaderNames(); e.hasMoreElements(); ) {
      String nextHeaderName = (String) e.nextElement();
      String headerValue = httpServletRequest.getHeader(nextHeaderName);
      if (!"authorization".equals(headerValue)) {
        requestHeaders.put(nextHeaderName, headerValue);
      }
    }
    return requestHeaders;
  }

  public static Map<String, String> getHeaderMapFromServerHttpRequest(
      ServerHttpRequest serverHttpRequest) {
    return serverHttpRequest.getHeaders().entrySet().stream().map(
            stringListEntry -> new SimpleEntry<String, String>(stringListEntry.getKey(),
                String.join(",", stringListEntry.getValue())) {
            }).filter(simpleEntry -> !simpleEntry.getKey().equals("authorization"))
        .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
  }
}
