package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.request.RasterIngestionRequest;
import com.revealprecision.revealserver.api.v1.dto.request.RasterStatisticsCalculationRequest;
import com.revealprecision.revealserver.api.v1.dto.response.MapLayerResponse;
import com.revealprecision.revealserver.api.v1.dto.response.RasterIngestionStatusResponse;
import com.revealprecision.revealserver.service.RasterService;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/raster")
public class RasterController {

  private final RasterService rasterService;

  @PostMapping("/ingest")
  public ResponseEntity<Void> ingest(@Valid @RequestBody RasterIngestionRequest request) {
    log.info("Received ingestion request taskIdentifier={}", request.getRasterId());
    rasterService.ingest(request);
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/statistics/calculate")
  public ResponseEntity<Void> calculateStatistics(@Valid @RequestBody RasterStatisticsCalculationRequest request) {
    log.info("Received statistics calculation request for rasterId={}", request.getRasterId());
    rasterService.calculateStatistics(request);
    return ResponseEntity.accepted().build();
  }

  @GetMapping("/{rasterId}/status")
  public ResponseEntity<RasterIngestionStatusResponse> getStatus(@PathVariable String rasterId) {
    log.info("Checking status for taskIdentifier={}", rasterId);
    return ResponseEntity.ok(rasterService.getIngestionStatus(rasterId));
  }


  @GetMapping("/map-layers")
  public ResponseEntity<List<MapLayerResponse>> getActiveMapLayers() {
    log.info("Fetching active map layers");
    return ResponseEntity.ok(rasterService.getActiveMapLayers());
  }

  @GetMapping("/tiles/{rasterId}/{z}/{x}/{y}.mvt")
  public ResponseEntity<byte[]> getTile(
      @PathVariable int z,
      @PathVariable int x,
      @PathVariable int y,
      @PathVariable String rasterId) {
    log.info("Fetching tile for rasterId={}, z={}, x={}, y={}", rasterId, z, x, y);
    byte[] tile = rasterService.getTile(rasterId, z, x, y);

    if(tile == null)
      return ResponseEntity.notFound().build();

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, "application/x-protobuf")
        .body(tile);
  }
}
