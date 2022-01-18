package com.revealprecision.revealserver.batch;

import lombok.experimental.UtilityClass;

public class BatchConstants {

  public static final String[] userFields = new String[]{
      "firstName",
      "lastName",
      "username",
      "email",
      "password",
      "tempPassword",
      "securityGroups",
      "organizations"
  };

  @UtilityClass
  public static class Constraint {

    public final String USERNAME_REGEX = "^[a-z]+([._]?[a-z]+){4,}$";
    public final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
  }
}
