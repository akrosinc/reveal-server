package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.TaskDetailsResponse;
import com.revealprecision.revealserver.constants.FormConstants;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.projection.LocationBusinessStateCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskDetailsService {
    private final LocationBusinessStatusService locationBusinessStatusService;
    private final LocationHierarchyService locationHierarchyService;
    private final PlanService planService;

    public TaskDetailsResponse getReportDataForLocation(UUID planId, UUID parentLocationId) {
//        UUID defaultHierarchyId = locationHierarchyService.getDefaultHierarchy().getIdentifier();

        Plan plan = planService.getPlanByIdentifier(planId);
        LocationHierarchy locationHierarchy = plan.getLocationHierarchy();

        Set<LocationBusinessStateCount> businessStateCounts = locationBusinessStatusService.getLocationBusinessStateObjPerGeoLevel
                (planId, parentLocationId, LocationConstants.STRUCTURE, locationHierarchy.getIdentifier());

        Map<String, Long> statusCounts = businessStateCounts.stream()
                .collect(Collectors.groupingBy(
                        LocationBusinessStateCount::getTaskBusinessStatus,
                        Collectors.summingLong(LocationBusinessStateCount::getLocationCount)
                ));

        long totalStructures = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        long totalVisited = statusCounts.getOrDefault(FormConstants.BusinessStatus.COMPLETE, 0L) + statusCounts.getOrDefault(FormConstants.BusinessStatus.VISITED, 0L) + statusCounts.getOrDefault(FormConstants.BusinessStatus.INCOMPLETE, 0L);
        long totalComplete = statusCounts.getOrDefault(FormConstants.BusinessStatus.COMPLETE, 0L);
        long totalIncomplete = statusCounts.getOrDefault(FormConstants.BusinessStatus.INCOMPLETE, 0L);

        double completionCoverage = totalVisited > 0 ? (double) totalComplete / totalVisited * 100 : 0.0;
        double visitationCoverage = totalStructures > 0 ? (double) totalVisited / totalStructures * 100 : 0.0;

        return TaskDetailsResponse.builder()
                .locationId(parentLocationId)
                .totalStructures(totalStructures)
                .totalVisited(totalVisited)
                .totalNotVisited(totalStructures - totalVisited)
                .totalComplete(totalComplete)
                .totalIncomplete(totalIncomplete)
                .completionCoverage(completionCoverage)
                .visitationCoverage(visitationCoverage)
                .build();
    }
}
