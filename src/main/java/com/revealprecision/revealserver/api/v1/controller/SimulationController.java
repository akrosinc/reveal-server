package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.request.DatasetLocationsRequest;
import com.revealprecision.revealserver.api.v1.dto.request.UpdateDatasetRequest;
import com.revealprecision.revealserver.api.v1.dto.request.SimulationDatasetRequest;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.api.v1.dto.response.SimulationDatasetResponse;
import com.revealprecision.revealserver.persistence.domain.Simulation;
import com.revealprecision.revealserver.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @GetMapping("{planId}")
    public ResponseEntity<Simulation> getSimulationByPlanId(@PathVariable UUID planId) {
        Simulation simulation = simulationService.getOrCreateSimulationByPlanId(planId);
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
}
