package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.request.DatasetLocationsRequest;
import com.revealprecision.revealserver.api.v1.dto.request.SimulationDatasetRequest;
import com.revealprecision.revealserver.api.v1.dto.request.UpdateDatasetRequest;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.api.v1.dto.response.SimulationDatasetResponse;
import com.revealprecision.revealserver.api.v1.dto.response.SimulationResponse;
import com.revealprecision.revealserver.persistence.domain.Simulation;
import com.revealprecision.revealserver.service.SimulationService;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @GetMapping("{planId}")
    public ResponseEntity<SimulationResponse> getSimulationByPlanId(@PathVariable UUID planId) {
        SimulationResponse simulation = simulationService.getSimulationWithTargetAreas(planId);
        return ResponseEntity.ok(simulation);
    }

    @PostMapping("dataset")
    public ResponseEntity<SimulationDatasetResponse> addDatasetToSimulation(
            @RequestBody SimulationDatasetRequest request) {
        return ResponseEntity.ok(simulationService.addDatasetToSimulation(request));
    }

    @PutMapping("dataset")
    public ResponseEntity<Simulation> updateSimulationDataset(@RequestBody UpdateDatasetRequest request) {
        return ResponseEntity.ok(simulationService.updateSimulationDataset(request));
    }

    @DeleteMapping("dataset")
    public ResponseEntity<Simulation> deleteSimulationDataset(@RequestBody UpdateDatasetRequest request) {
        return ResponseEntity.ok(simulationService.deleteSimulationDataset(request));
    }

    //TODO: move to location controller
    @PostMapping("dataset/location-data")
    public ResponseEntity<List<LocationResponse>> getDatasetDataForLocations(
            @RequestBody DatasetLocationsRequest request) throws IOException {
        return ResponseEntity.ok(simulationService.getDatasetDataForLocations(request));
    }

    @PostMapping("/add-search-request")
    public String addSearchRequest(
            @RequestBody SimulationDatasetRequest request) {
        return simulationService.filterDatasetsPerAdminLevel(request);
    }

    @GetMapping("/datasets/filter-sse")
    public SseEmitter addSearchRequest(
            @RequestParam("searchId") String searchId) {
        return simulationService.getDatasetDataForLocations(searchId);
    }

    @GetMapping("/within")
    public ResponseEntity<List<LocationResponse>> getStructuresWithinBoundingBox(
            @RequestParam double topLeftLon,
            @RequestParam double topLeftLat,
            @RequestParam double bottomRightLon,
            @RequestParam double bottomRightLat) {
        return ResponseEntity.ok(simulationService.getStructuresWithinBoundingBox(topLeftLon, topLeftLat, bottomRightLon, bottomRightLat));
    }
}