package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.exceptions.RasterProcessingException;
import com.revealprecision.revealserver.messaging.message.RasterIngestionMessage;
import com.revealprecision.revealserver.props.RasterIngestionProperties;
import com.revealprecision.revealserver.raster.TiffProcessor;
import com.revealprecision.revealserver.raster.TiffProcessor.TilingException;
import com.revealprecision.revealserver.service.models.RasterLocationPaths;
import com.revealprecision.revealserver.raster.RasterUtil;
import java.io.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TileGenerator {

  private final RasterIngestionProperties properties;

  public void generateTiles(RasterIngestionMessage message) {
    RasterLocationPaths paths = RasterUtil.validateAndResolvePaths(message.getRasterId(), properties);

    String inputPath = paths.getCogPath() != null && new File(paths.getCogPath()).exists()
        ? paths.getCogPath()
        : paths.getRasterFilePath();

    TiffProcessor.Config config = TiffProcessor.Config.builder()
        .cogPath(inputPath)
        .outputDir(paths.getTilesDirectoryPath())
        .minZoom(message.getMinZoom() != null ? message.getMinZoom() : 0)
        .maxZoom(message.getMaxZoom() != null ? message.getMaxZoom() : 14)
        .layerName(message.getLayerName())
        .valueAttribute(message.getValueAttribute())
        .threadCount(8)
        .gzip(false)
        .build();

    try {
      TiffProcessor.Result result = new TiffProcessor(config).run();
      log.info("Tile generation complete: considered={}, written={}, skippedEmpty={}, failed={}, elapsed={}ms",
          result.tilesConsidered(),
          result.tilesWritten(),
          result.tilesSkippedEmpty(),
          result.tilesFailed(),
          result.elapsedMs());
    } catch (TilingException e) {
      throw new RasterProcessingException("Failed to generate tiles for raster: " + message.getRasterId(), e);
    }
  }
}
