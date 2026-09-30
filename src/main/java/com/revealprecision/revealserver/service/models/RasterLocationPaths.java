package com.revealprecision.revealserver.service.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class RasterLocationPaths {
  String rasterFilePath;
  String tilesDirectoryPath;
  String cogPath;

  public String getCogFilePath() {
    return cogPath;
  }
}
