package com.revealprecision.revealserver.api.v1.dto;

import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class TaskDetailsResponse {
    private UUID locationId;
    private Long totalStructures;
    private Long totalVisited;
    private Long totalNotVisited;
    private Long totalComplete;
    private Long totalIncomplete;
    private Double visitationCoverage;
    private Double completionCoverage;
    // STRUCTURES ONLY???
}
