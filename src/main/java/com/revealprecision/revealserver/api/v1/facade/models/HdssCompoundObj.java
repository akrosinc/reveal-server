package com.revealprecision.revealserver.api.v1.facade.models;


import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HdssCompoundObj implements Serializable {

  Set<HdssCompound> allCompounds;

  Set<HdssCompoundHousehold> compoundHouseHolds;

  Set<HdssHouseholdIndividual> allHouseholdIndividual;

  Set<HdssHouseholdStructure> allHouseholdStructure;

  Set<HdssIndividual> allIndividuals;

  boolean isEmpty;

  long serverVersion;

  @Data
  @Builder
  public static class HdssCompound implements Serializable {

    private long serverVersion;
    private String compoundId;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      HdssCompound that = (HdssCompound) o;
      return serverVersion == that.serverVersion && compoundId.equals(that.compoundId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(serverVersion, compoundId);
    }
  }

  @Data
  @Builder
  public static class HdssCompoundHousehold implements Serializable {

    private long serverVersion;
    private String compoundId;
    private String householdId;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      HdssCompoundHousehold that = (HdssCompoundHousehold) o;
      return serverVersion == that.serverVersion && compoundId.equals(that.compoundId)
          && householdId.equals(that.householdId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(serverVersion, compoundId, householdId);
    }
  }

  @Data
  @Builder
  public static class HdssHouseholdIndividual implements Serializable {

    private long serverVersion;
    private String householdId;
    private String individualId;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      HdssHouseholdIndividual that = (HdssHouseholdIndividual) o;
      return serverVersion == that.serverVersion && householdId.equals(that.householdId)
          && individualId.equals(that.individualId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(serverVersion, householdId, individualId);
    }
  }

  @Data
  @Builder
  public static class HdssHouseholdStructure implements Serializable {

    private long serverVersion;
    private String householdId;
    private String structureId;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      HdssHouseholdStructure that = (HdssHouseholdStructure) o;
      return serverVersion == that.serverVersion && householdId.equals(that.householdId)
          && structureId.equals(that.structureId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(serverVersion, householdId, structureId);
    }
  }

  @Data
  @Builder
  public static class HdssIndividual implements Serializable {

    private long serverVersion;
    private String identifier;
    private String individualId;
    private String dob;
    private String gender;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      HdssIndividual that = (HdssIndividual) o;
      return serverVersion == that.serverVersion && identifier.equals(that.identifier)
          && individualId.equals(that.individualId) && dob.equals(that.dob) && gender.equals(
          that.gender);
    }

    @Override
    public int hashCode() {
      return Objects.hash(serverVersion, identifier, individualId, dob, gender);
    }
  }
}
