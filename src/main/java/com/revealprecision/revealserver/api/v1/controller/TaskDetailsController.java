package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.TaskDetailsResponse;
import com.revealprecision.revealserver.service.TaskDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/task-details")
public class TaskDetailsController {
    private final TaskDetailsService taskDetailsService;

    @GetMapping
    public ResponseEntity<TaskDetailsResponse> getTaskDataForLocation(@RequestParam("planId")UUID planId, @RequestParam("locationId") UUID locationId) {
        return ResponseEntity.ok(taskDetailsService.getReportDataForLocation(planId, locationId));
    }
}
