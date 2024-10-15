package com.revealprecision.revealserver.api.v1.facade.models;


import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HdssCompoundHouseholdIndividualObj implements Serializable {

        private String id;
        private String individualId;
        private String compoundId;
        private String householdId;
        private String dob;
        private String gender;

}
