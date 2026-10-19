package com.revealprecision.revealserver.api.v1.dto.response;

import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RasterDatasetResponse {

  private UUID identifier;
  private String datasetIdentifier;
  private String name;
  private String colorRamp;
  private MapLayerResponse mapLayer;
  private Map<String, Object> metadata;
}
