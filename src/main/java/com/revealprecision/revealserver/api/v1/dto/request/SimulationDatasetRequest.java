package com.revealprecision.revealserver.api.v1.dto.request;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SimulationDatasetRequest {
    UUID simulationId;
    UUID tagId;
    String hexColor;
    Integer lineWidth;
    UUID parentLocationId;
    List<String> tagsIds;
}