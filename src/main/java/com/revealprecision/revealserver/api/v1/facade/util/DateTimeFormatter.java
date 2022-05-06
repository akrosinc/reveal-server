package com.revealprecision.revealserver.api.v1.facade.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import org.joda.time.DateTime;

public class DateTimeFormatter {


  public static String getDateTimeFacadeStringFromDate(Date date) {

    return getDateTimeFacadeStringFromLocalDateTime(
        date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
  }


  public static String getDateTimeFacadeStringFromLocalDateTime(LocalDateTime localDateTime) {

    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss.SSS");

    String formatDateTime = localDateTime.format(formatter);
    java.time.format.DateTimeFormatter dateTimeFormatter = java.time.format.DateTimeFormatter.ISO_INSTANT
        .withZone(
            ZoneId.systemDefault());

    ZonedDateTime zonedDateTime = LocalDateTime.parse(formatDateTime, formatter)
        .atZone(ZoneId.systemDefault());

    return zonedDateTime.format(dateTimeFormatter);
  }

  public static String getDateTimeAndroidFacadeStringFromLocalDateTime(
      LocalDateTime localDateTime) {

    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HHmm");

    String formatDateTime = localDateTime.format(formatter);

    return formatDateTime.replace(" ", "T");
  }


  public static LocalDateTime getLocalDateTimeFromAndroidFacadeString(
      String androidFacadeDateString) {

    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HHmm");

    return LocalDateTime.parse(androidFacadeDateString.replace("T", " "), formatter);
  }

  public static LocalDateTime getLocalDateTimeFromZonedAndroidFacadeString(
      String androidFacadeDateString) {
    //Note android uses joda time, we handle that here until we unify.
    return LocalDateTime.ofInstant(
        Instant.ofEpochMilli(org.joda.time.DateTime.parse(androidFacadeDateString).getMillis()),
        ZoneId.systemDefault());
  }


}
