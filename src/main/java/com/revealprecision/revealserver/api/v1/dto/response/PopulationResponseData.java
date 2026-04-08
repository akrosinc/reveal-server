package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PopulationResponseData {
    Double female;
    Double male;
    Double sum;
    @JsonProperty("Pyramids")
    List<PopulationPyramids> pyramids;
}
