package com.revealprecision.revealserver.api.v1.facade.models;


import java.io.Serializable;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HdssCompoundHouseholdIndividualPushObj implements Serializable {

        private String identifier;
        private String individualId;
        private String compoundId;
        private String householdId;
        private UUID structureId;
        private String dob;
        private String gender;
        private int serverVersion;
}
