package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PopulationPyramids {
    @JsonProperty("AgeGroup")
    private String ageGroup;
    @JsonProperty("MalePop")
    private Double malePopulation;
    @JsonProperty("FemalePop")
    private Double femalePopulation;
    @JsonProperty("TotalPop")
    private Double totalPopulation;

}
