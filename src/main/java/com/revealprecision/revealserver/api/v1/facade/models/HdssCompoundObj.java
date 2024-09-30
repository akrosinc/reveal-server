package com.revealprecision.revealserver.api.v1.facade.models;


import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HdssCompoundObj implements Serializable {

    List<HdssCompound> allCompounds;

    List<HdssCompoundHousehold> compoundHouseHolds;

    List<HdssHouseholdIndividual> allHouseholdIndividual;

    List<HdssHouseholdStructure> allHouseholdStructure;

    List<HdssIndividual> allIndividuals;

    @Data
    @Builder
    public static class HdssCompound implements Serializable {
        private String compoundId;
    }

    @Data
    @Builder
    public static class HdssCompoundHousehold implements Serializable{
        private String compoundId;
        private String householdId;
    }

    @Data
    @Builder
    public static class HdssHouseholdIndividual implements Serializable{
        private String householdId;
        private String individualId;
    }

    @Data
    @Builder
    public static class HdssHouseholdStructure implements Serializable{
        private String householdId;
        private String structureId;
    }

    @Data
    @Builder
    public static class HdssIndividual implements Serializable{
        private String identifier;
        private String individualId;
        private String dob;
        private String gender;
    }
}
