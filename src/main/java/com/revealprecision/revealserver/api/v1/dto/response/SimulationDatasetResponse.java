package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.enums.DatasetType;
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
    private DataSetYearRangeResponse dataSetYearRange;
    private DatasetType datasetType;
    private String colorRamp;
    private Map<String, Object> metadata;


    public SimulationDatasetResponse(UUID simulationId, UUID tagId, UUID datasetId, String datasetName,
                                     String hexColor, String borderColor, Integer lineWidth,
                                     Map<String, EntityMetadataResponse> locationWithMetadata,
                                     DataSetYearRangeResponse dataSetYearRange) {
        this.simulationId = simulationId;
        this.tagId = tagId;
        this.datasetId = datasetId;
        this.datasetName = datasetName;
        this.hexColor = hexColor;
        this.borderColor = borderColor;
        this.lineWidth = lineWidth;
        this.locationWithMetadata = locationWithMetadata;
        this.dataSetYearRange = dataSetYearRange;
    }
}