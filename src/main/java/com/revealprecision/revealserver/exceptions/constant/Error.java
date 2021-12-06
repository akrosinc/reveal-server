package com.revealprecision.revealserver.exceptions.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Error {

  public static final String RESOURCE_NOT_FOUND = "Resource '%s' with '%s' not found";
  public static final String NON_UNIQUE = "%s with value '%s' already exists";
}