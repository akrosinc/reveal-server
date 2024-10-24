package com.revealprecision.revealserver.persistence.projection;

public interface HdssIndividualProjection {

  long getServerVersion();

  String getId();

  String getIndividualId();

  java.sql.Date getDob();

  String getGender();
}
