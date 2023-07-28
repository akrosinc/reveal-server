package com.revealprecision.revealserver.api.v1.controller.querying;

import com.revealprecision.revealserver.api.v1.dto.request.SaveHierarchyRequest;
import com.revealprecision.revealserver.api.v1.dto.response.LocationHierarchyResponse;
import com.revealprecision.revealserver.api.v1.dto.response.SaveHierarchyResponse;
import com.revealprecision.revealserver.persistence.projection.LocationMainData;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.SimulationHierarchyService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/genericHierarchy")
@Profile("Elastic")
public class SimulationHierarchyController {

  private final SimulationHierarchyService saveSimulationHierarchy;
  private final LocationRelationshipService locationRelationshipService;


  @PostMapping("/saveSimulationHierarchy")
  public SaveHierarchyResponse saveSimulationHierarchy(
      @RequestBody SaveHierarchyRequest saveHierarchyRequest) {

    log.info("{}", saveHierarchyRequest);
    return saveSimulationHierarchy.saveSimulationHierarchy(saveHierarchyRequest);

  }

  @GetMapping("/simulationHierarchy")
  public List<LocationHierarchyResponse> getSimulationHierarchies() {

    return saveSimulationHierarchy.generatedHierarchies().stream()
        .map(generatedHierarchy -> LocationHierarchyResponse.builder()
            .identifier(String.valueOf(generatedHierarchy.getId()))
            .name(generatedHierarchy.getName())
            .nodeOrder(generatedHierarchy.getNodeOrder())
            .build()).collect(Collectors.toList());
  }

  @GetMapping("/{hierarchyId}/{type}/{geographicLevel}")
  public ResponseEntity<List<LocationMainData>> getSimulationHierarchies(
      @PathVariable String hierarchyId, @PathVariable String type,
      @PathVariable String geographicLevel) {

    List<LocationMainData> locationMainData;
    if (type.equals("saved")) {

      locationMainData = locationRelationshipService.getLocationsByHierarchyIdAndLevelName(
          UUID.fromString(hierarchyId), geographicLevel);
    } else {
      locationMainData = saveSimulationHierarchy.getLocationIdByHierarchyIdAndLevelName(
          hierarchyId,
          geographicLevel);
    }

    return ResponseEntity.ok(locationMainData);
  }


}
