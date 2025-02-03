package com.revealprecision.revealserver.api.v1.dto.response;

import lombok.*;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationDatasetResponse {
    private UUID simulationId;
    private UUID tagId;
    private UUID datasetId;
    private String datasetName;
    private String hexColor;
    private String borderColor;
    private Integer lineWidth;
    private Map<String, EntityMetadataResponse> locationWithMetadata;
}