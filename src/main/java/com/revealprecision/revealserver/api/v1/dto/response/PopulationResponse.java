package com.revealprecision.revealserver.api.v1.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PopulationResponse {
    List<PopulationResponseResults> results;
}
