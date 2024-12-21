package com.revealprecision.revealserver.persistence.projection;

public interface HdssCompoundHouseholdIndividualProjection {

  String getId();

  String getCompoundId();

  String getHouseholdId();

  String getIndividualId();

  String getName();

  String getDob();

  String getGender();

  long getServerVersion();

  String getStructureId();
}
