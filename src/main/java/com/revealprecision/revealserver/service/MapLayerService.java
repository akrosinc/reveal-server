package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.MapLayerResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.RasterIngestionRequest;
import com.revealprecision.revealserver.api.v1.dto.response.MapLayerResponse;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.LayerType;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.constant.Error;
import com.revealprecision.revealserver.model.GeoEnvelope;
import com.revealprecision.revealserver.persistence.domain.MapLayer;
import com.revealprecision.revealserver.persistence.repository.MapLayerRepository;
import com.revealprecision.revealserver.props.RasterIngestionProperties;
import com.revealprecision.revealserver.raster.RasterUtil;
import com.revealprecision.revealserver.service.models.RasterLocationPaths;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class MapLayerService {

  private final MapLayerRepository mapLayerRepository;
  private final RasterIngestionProperties rasterIngestionProperties;

  public MapLayer createMapLayer(RasterIngestionRequest request) {
    String name = request.getName();
    if (name == null || name.isBlank()) {
      if (request.getMetadata() != null && request.getMetadata().get("name") != null) {
        name = request.getMetadata().get("name").toString();
      } else {
        name = request.getRasterId();
      }
    }
    return createMapLayer(request.getRasterId(), name);
  }

  public MapLayer createMapLayer(String rasterId, String name) {
    String mapLayerName = name != null && !name.isBlank() ? name : rasterId;

    if (mapLayerRepository.findByLayerIdentifier(rasterId).isPresent()) {
      throw new ConflictException(
          String.format(Error.NON_UNIQUE, MapLayer.Fields.layerIdentifier, rasterId));
    }

    if (mapLayerRepository.findByName(mapLayerName).isPresent()) {
      throw new ConflictException(
          String.format(Error.NON_UNIQUE, MapLayer.Fields.name, mapLayerName));
    }

    RasterLocationPaths paths = RasterUtil.validateAndResolvePaths(rasterId, rasterIngestionProperties);
    GeoEnvelope extent = RasterUtil.getRasterExtent(paths.getRasterFilePath());

    MapLayer mapLayer = MapLayer.builder()
        .name(mapLayerName)
        .layerIdentifier(rasterId)
        .type(LayerType.RASTER)
        .extent(extent)
        .build();
    mapLayer.setEntityStatus(EntityStatus.CREATING);
    return mapLayerRepository.save(mapLayer);
  }

  @Transactional
  public void activateMapLayer(String rasterId) {
    mapLayerRepository.getByLayerIdentifier(rasterId).ifPresent(mapLayer -> {
      mapLayer.setEntityStatus(EntityStatus.ACTIVE);
      mapLayerRepository.save(mapLayer);
      log.info("MapLayer activated for rasterId: {}", rasterId);
    });
  }

  public List<MapLayerResponse> getActiveMapLayers() {
    return MapLayerResponseFactory.fromEntityList(
        mapLayerRepository.findByEntityStatus(EntityStatus.ACTIVE));
  }
}
