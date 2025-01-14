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
    UUID simulationId;
    @NotNull
    UUID datasetId;
    String name;
    String hexColor;
    Integer lineWidth;
}
