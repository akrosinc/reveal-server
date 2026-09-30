package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.MapLayerResponse;
import com.revealprecision.revealserver.model.GeoEnvelope;
import com.revealprecision.revealserver.persistence.domain.MapLayer;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MapLayerResponseFactory {

  public static MapLayerResponse fromEntity(MapLayer mapLayer) {
    if (mapLayer == null) {
      return null;
    }

    return MapLayerResponse.builder()
        .id(mapLayer.getId())
        .name(mapLayer.getName())
        .layerIdentifier(mapLayer.getLayerIdentifier())
        .type(mapLayer.getType())
        .extent(mapLayer.getExtent())
        .build();
  }

  public static List<MapLayerResponse> fromEntityList(List<MapLayer> mapLayers) {
    if (mapLayers == null) {
      return Collections.emptyList();
    }
    return mapLayers.stream()
        .map(MapLayerResponseFactory::fromEntity)
        .collect(Collectors.toList());
  }
}
