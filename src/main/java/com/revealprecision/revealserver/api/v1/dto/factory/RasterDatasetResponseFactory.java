package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.RasterDatasetResponse;
import com.revealprecision.revealserver.persistence.domain.RasterDataset;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RasterDatasetResponseFactory {

  public static RasterDatasetResponse fromEntity(RasterDataset rasterDataset) {
    if (rasterDataset == null) {
      return null;
    }

    return RasterDatasetResponse.builder()
        .identifier(rasterDataset.getIdentifier())
        .datasetIdentifier(rasterDataset.getDatasetIdentifier())
        .name(rasterDataset.getName())
        .colorRamp(rasterDataset.getColorRamp())
        .mapLayer(MapLayerResponseFactory.fromEntity(rasterDataset.getMapLayer()))
        .build();
  }

  public static List<RasterDatasetResponse> fromEntityList(List<RasterDataset> rasterDatasets) {
    if (rasterDatasets == null) {
      return Collections.emptyList();
    }

    return rasterDatasets.stream()
        .map(RasterDatasetResponseFactory::fromEntity)
        .collect(Collectors.toList());
  }
}
