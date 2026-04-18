package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.Map;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SimulationDatasetRequest {
    private UUID simulationId;
    private UUID tagId;
    private String hexColor;
    private Integer lineWidth;
    private String borderColor;
    private UUID parentLocationId;
    private String parentAdminLevel;
    private Map<String, Integer> dataSetYearFilter;
    private Boolean addToSimulation;
    private List<UUID> userDatasetIds;
}