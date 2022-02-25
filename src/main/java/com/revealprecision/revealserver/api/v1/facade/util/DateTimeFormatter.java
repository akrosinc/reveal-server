package com.revealprecision.revealserver.api.v1.facade.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DateTimeFormatter {


  public static String getDateTimeFacadeStringFromLocalDateTime(LocalDateTime localDateTime){

    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    String formatDateTime = localDateTime.format(formatter);
    java.time.format.DateTimeFormatter dateTimeFormatter = java.time.format.DateTimeFormatter.ISO_INSTANT.withZone(
        ZoneId.systemDefault());

    ZonedDateTime zonedDateTime = LocalDateTime.parse(formatDateTime,formatter).atZone(ZoneId.systemDefault());

    return zonedDateTime.format(dateTimeFormatter);
  }

}
