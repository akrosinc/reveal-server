package com.revealprecision.revealserver.exceptions.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Error {

  public static final String RESOURCE_NOT_FOUND = "Resource '%s' with '%s' not found";
  public static final String NON_UNIQUE = "%s with value '%s' already exists";
  public static final String WRONG_ENUM = "Enum '%s' with value '%s' does not exist";
  public static final String RESOURCES_NOT_WIRED = "Resource '%s' with identifier: %s does not belong to resource '%s' with identifier '%s'";
  public static final String WRONG_FORMAT = "Resource '%s' with value '%s' does not have format of '%s'";
}
