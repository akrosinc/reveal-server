package com.revealprecision.revealserver.persistence.projection;

public interface HdssCompoundHouseholdIndividualProjection {

  String getId();

  String getCompoundId();

  String getHouseholdId();

  String getIndividualId();

  java.sql.Date getDob();

  String getGender();

  long getServerVersion();

  String getStructureId();
}
