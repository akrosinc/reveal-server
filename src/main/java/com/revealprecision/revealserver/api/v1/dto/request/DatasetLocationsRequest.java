package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DatasetLocationsRequest {
    private UUID simulationId;
    private UUID planId;
    private List<UUID> datasetsIds;
    private Boolean includeGeometry;
    private UUID parentLocationId;
    private Boolean campaignManagementFeatures;
    private Map<UUID, Integer> dataSetYearFilter;
}