package com.revealprecision.revealserver.api.v1.dto.request;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PopulationRequest {
    private String type;
    private List<Object> coordinates;
}
