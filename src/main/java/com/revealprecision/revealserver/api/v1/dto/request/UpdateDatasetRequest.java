package com.revealprecision.revealserver.api.v1.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDatasetRequest {
    @NotNull
    private UUID simulationId;
    @NotNull
    private UUID datasetId;
    private String name;
    private String hexColor;
    private Integer lineWidth;
    private String borderColor;
}
