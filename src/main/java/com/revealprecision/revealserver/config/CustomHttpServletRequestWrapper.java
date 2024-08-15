package com.revealprecision.revealserver.config;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class CustomHttpServletRequestWrapper extends HttpServletRequestWrapper {
  private final Map<String, String> customHeaders;

  public CustomHttpServletRequestWrapper(HttpServletRequest request) {
    super(request);
    this.customHeaders = new HashMap<>();
  }

  @Override
  public String getHeader(String name) {
    if (customHeaders.containsKey(name)) {
      return customHeaders.get(name);
    }
    return super.getHeader(name);
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    if (customHeaders.containsKey(name)) {
      return Collections.enumeration(Collections.singleton(customHeaders.get(name)));
    }
    return super.getHeaders(name);
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    Enumeration<String> superNames = super.getHeaderNames();
    return new Enumeration<String>() {
      private String next = findNext();

      private String findNext() {
        while (superNames.hasMoreElements()) {
          String headerName = superNames.nextElement();
          if (!customHeaders.containsKey(headerName)) {
            return headerName;
          }
        }
        return null;
      }

      @Override
      public boolean hasMoreElements() {
        return next != null;
      }

      @Override
      public String nextElement() {
        String current = next;
        next = findNext();
        return current;
      }
    };
  }

  public void addHeader(String name, String value) {
    customHeaders.put(name, value);
  }
}
