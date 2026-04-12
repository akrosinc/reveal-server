package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.persistence.domain.Dataset;
import java.util.HashMap;
import java.util.Map;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SimulationResponse {
    private UUID identifier;
    private List<Dataset> datasets = new ArrayList<>();
    private List<LocationResponse> targetAreas = new ArrayList<>();

    private List<DataSetYearRangeResponse> datSetYearRange = new ArrayList<>();
}
