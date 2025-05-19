package com.revealprecision.revealserver.api.v1.facade.models;


import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HdssCompoundObj implements Serializable {

  Set<HdssCompound> allCompounds;

  Set<HdssCompoundHousehold> compoundHouseHolds;

  Set<HdssHouseholdIndividual> allHouseholdIndividual;

  Set<HdssHouseholdStructure> allHouseholdStructure;

  Set<HdssIndividual> allIndividuals;

  Set<HdssHousehold> allHouseholds;

  Set<String> allHouseholdIndividualToDelete;

  Set<String> allCompoundHouseholdToDelete;

  boolean isEmpty;

  long serverVersion;

  int totalRecords;

  @Data
  @Builder
  public static class HdssCompound implements Serializable {

    private long serverVersion;
    private String compoundId;

  }

  @Data
  @Builder
  public static class HdssCompoundHousehold implements Serializable {

    private long serverVersion;
    private String compoundId;
    private String householdId;

  }

  @Data
  @Builder
  public static class HdssHouseholdIndividual implements Serializable {

    private long serverVersion;
    private String householdId;
    private String individualId;

  }

  @Data
  @Builder
  public static class HdssHouseholdStructure implements Serializable {

    private long serverVersion;
    private String householdId;
    private String structureId;

  }

  @Data
  @Builder
  public static class HdssIndividual implements Serializable {

    private long serverVersion;
    private String identifier;
    private String individualId;
    private String dob;
    private String gender;
    private String name;
    private String cluster;
    private String floatingLocationId;
    private String floatingLocationName;
    private String floatingLocationGeographicLevel;
  }

  @Data
  @Builder
  public static class HdssHousehold implements Serializable {

    private long serverVersion;
    private String householdId;
    private String floatingHouseholdLocationName;

  }
}
