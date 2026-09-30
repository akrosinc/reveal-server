package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.MapLayerResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.RasterIngestionRequest;
import com.revealprecision.revealserver.api.v1.dto.request.RasterStatisticsCalculationRequest;
import com.revealprecision.revealserver.api.v1.dto.response.MapLayerResponse;
import com.revealprecision.revealserver.api.v1.dto.response.RasterIngestionStatusResponse;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.LayerType;
import com.revealprecision.revealserver.exceptions.InvalidRasterEventException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.RasterProcessingException;
import com.revealprecision.revealserver.messaging.message.RasterIngestionMessage;
import com.revealprecision.revealserver.messaging.message.RasterLocationZonalStatsMessage;
import com.revealprecision.revealserver.persistence.domain.IngestionTask;
import com.revealprecision.revealserver.persistence.domain.MapLayer;
import com.revealprecision.revealserver.persistence.repository.IngestionTaskRepository;
import com.revealprecision.revealserver.persistence.repository.MapLayerRepository;
import com.revealprecision.revealserver.model.GeoEnvelope;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.props.RasterIngestionProperties;
import com.revealprecision.revealserver.raster.CogBuilder;
import com.revealprecision.revealserver.service.models.RasterLocationPaths;
import com.revealprecision.revealserver.raster.RasterUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class RasterService {

  private final PublisherService publisherService;
  private final KafkaProperties kafkaProperties;
  private final IngestionTaskRepository ingestionTaskRepository;
  private final MapLayerRepository mapLayerRepository;
  private final RasterIngestionProperties rasterIngestionProperties;
  private final MapLayerService mapLayerService;
  private final IngestionTaskService ingestionTaskService;

  @Transactional
  public void ingest(@Valid RasterIngestionRequest request) {

    createMapLayer(request);

    if (request.isConvertToCog()) {
      log.info("Converting to COG");
      RasterLocationPaths paths = RasterUtil.validateAndResolvePaths(request.getRasterId(), rasterIngestionProperties);
      String rasterFilePath = paths.getRasterFilePath();
      String cogPath = paths.getCogPath();
      CogBuilder cogBuilder = new CogBuilder();
      cogBuilder.build(rasterFilePath, cogPath);
    }

    int minZoom = request.getMinZoom() != null ? request.getMinZoom() : 0;
    int maxZoom = request.getMaxZoom() != null ? request.getMaxZoom() : 14;
    int totalSteps = maxZoom - minZoom + 1;

    ingestionTaskService.initIngestionTask(request.getRasterId(), totalSteps);

    RasterIngestionMessage rasterIngestionMessage = null;

    for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
      rasterIngestionMessage =  RasterIngestionMessage.builder()
          .rasterId(request.getRasterId())
          .minZoom(zoom)
          .maxZoom(zoom)
          .layerName(request.getLayerName())
          .valueAttribute(request.getValueAttribute())
          .metadata(request.getMetadata())
          .build();

      publisherService.send(kafkaProperties.getTopicMap().get(KafkaConstants.RASTER_INGESTION),
          request.getRasterId(),
          rasterIngestionMessage);
    }

    RasterStatisticsCalculationRequest statsConfig = request.getStatisticsCalculation();

    if(statsConfig == null)
        return;

    calculateStatistics(statsConfig);
  }

  public void calculateStatistics(RasterStatisticsCalculationRequest request) {

    if(request == null)
      return;

    RasterLocationZonalStatsMessage rasterLocationZonalStatsMessage = RasterLocationZonalStatsMessage.builder()
        .rasterId(request.getRasterId())
        .reprocess(request.getReprocess())
        .geographicLevels(request.getGeographicLevels())
        .build();

    publisherService.send(kafkaProperties.getTopicMap().get(KafkaConstants.RASTER_LOCATION_ZONAL_STATS),
        request.getRasterId(),
        rasterLocationZonalStatsMessage);
  }


  public RasterIngestionStatusResponse getIngestionStatus(String rasterId) {
    IngestionTask status = ingestionTaskRepository.findByTaskIdentifier(rasterId)
        .orElseThrow(() -> new NotFoundException("Status not found for taskIdentifier: " + rasterId));

    return RasterIngestionStatusResponse.builder()
        .stage(status.getStage())
        .message(status.getMessage())
        .lastUpdated(status.getLastUpdated())
        .build();
  }

  public MapLayer createMapLayer(RasterIngestionRequest request) {
    return mapLayerService.createMapLayer(request);
  }

  public MapLayer createMapLayer(String rasterId, String name) {
    return mapLayerService.createMapLayer(rasterId, name);
  }

  @Transactional
  public void activateMapLayer(String rasterId) {
    mapLayerService.activateMapLayer(rasterId);
  }

  public byte[] getTile(String rasterId, int z, int x, int y) {

    if (rasterId.contains("..") || rasterId.contains("/") || rasterId.contains("\\")) {
      throw new InvalidRasterEventException("Invalid rasterId: " + rasterId);
    }

    Path basePath = Paths.get(rasterIngestionProperties.getBasePath()).toAbsolutePath().normalize();
    Path rasterFolder = basePath.resolve(rasterId).normalize();

    if (!rasterFolder.startsWith(basePath)) {
      throw new InvalidRasterEventException("Access denied: path escapes base directory");
    }

    Path tileFile = rasterFolder
        .resolve(rasterIngestionProperties.getTilesSubdirName())
        .resolve(String.valueOf(z))
        .resolve(String.valueOf(x))
        .resolve(y + ".mvt");

    try {
      return Files.readAllBytes(tileFile);
    } catch (IOException e) {
      throw new RasterProcessingException("Failed to read tile file: " + tileFile, e);
    }
  }

  public List<MapLayerResponse> getActiveMapLayers() {
    return mapLayerService.getActiveMapLayers();
  }
}
