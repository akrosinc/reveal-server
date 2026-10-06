package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.model.ZonalStatistics;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.RasterLocationZonalStats;
import com.revealprecision.revealserver.persistence.repository.RasterLocationZonalStatsRepository;
import com.revealprecision.revealserver.props.RasterIngestionProperties;
import com.revealprecision.revealserver.service.models.RasterLocationPaths;
import com.revealprecision.revealserver.raster.GdalBootstrap;
import com.revealprecision.revealserver.raster.RasterUtil;
import com.revealprecision.revealserver.raster.ZonalStatsUtil;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.ogr.Geometry;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osrConstants;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RasterLocationZonalStatsService {

  private final RasterIngestionProperties properties;
  private final RasterLocationZonalStatsRepository rasterLocationZonalStatsRepository;
  private final ObjectMapper objectMapper;

  public void calculateLocationZonalStats(String rasterId, List<Location> locations, boolean reprocess, String tag) {
    if (locations == null || locations.isEmpty()) {
      return;
    }

    Set<UUID> existingLocationIds = null;
    if (!reprocess) {
      existingLocationIds = rasterLocationZonalStatsRepository.findLocationIdentifiersByRasterId(rasterId);
    }

    RasterLocationPaths paths;
    try {
      paths = RasterUtil.validateAndResolvePaths(rasterId, properties);
    } catch (Exception e) {
      log.error("Failed to resolve raster paths for rasterId {}: {}", rasterId, e.getMessage());
      return;
    }

    String rasterPath = paths.getRasterFilePath();
    GdalBootstrap.init();
    Dataset probeDataset = gdal.Open(rasterPath, gdalconstConstants.GA_ReadOnly);
    if (probeDataset == null) {
      log.error("Could not open raster dataset: {} - {}", rasterPath, gdal.GetLastErrorMsg());
      return;
    }
    probeDataset.delete();

    Set<GdalContext> openContexts = ConcurrentHashMap.newKeySet();
    ThreadLocal<GdalContext> threadLocalContext = ThreadLocal.withInitial(() -> {
      GdalContext ctx = new GdalContext(rasterPath);
      openContexts.add(ctx);
      return ctx;
    });

    final Set<UUID> finalExistingLocationIds = existingLocationIds;
    ConcurrentLinkedQueue<RasterLocationZonalStats> results = new ConcurrentLinkedQueue<>();

    try {
      locations.parallelStream().forEach(location -> {
        if (location == null) {
          return;
        }
        if (!reprocess && finalExistingLocationIds != null && location.getIdentifier() != null
            && finalExistingLocationIds.contains(location.getIdentifier())) {
          log.debug("Skipping already processed location {} for rasterId {} (reprocess=false)",
              location.getIdentifier(), rasterId);
          return;
        }
        try {
          GdalContext ctx = threadLocalContext.get();
          calculateSingleLocation(
              rasterId, location, ctx.dataset, ctx.rasterSRS, ctx.toRasterCrs, ctx.rasterExtent, tag)
              .ifPresent(results::add);
        } catch (Exception e) {
          log.error("Error calculating zonal stats for location {}: {}",
              location.getIdentifier(), e.getMessage(), e);
        }
      });
    } finally {
      threadLocalContext.remove();
      for (GdalContext ctx : openContexts) {
        try {
          ctx.close();
        } catch (Exception e) {
          log.warn("Error closing GDAL context for rasterId {}: {}", rasterId, e.getMessage());
        }
      }
    }

    if (!results.isEmpty()) {
      rasterLocationZonalStatsRepository.saveAll(results);
      log.info("Persisted zonal stats for {} location(s) for rasterId {}", results.size(), rasterId);
    }
  }

  private static class GdalContext implements AutoCloseable {
    final Dataset dataset;
    final SpatialReference rasterSRS;
    final SpatialReference wgs84;
    final CoordinateTransformation toRasterCrs;
    final Geometry rasterExtent;

    GdalContext(String rasterPath) {
      this.dataset = gdal.Open(rasterPath, gdalconstConstants.GA_ReadOnly);
      if (this.dataset == null) {
        throw new IllegalStateException(
            "Could not open raster dataset: " + rasterPath + " - " + gdal.GetLastErrorMsg());
      }
      this.rasterSRS = new SpatialReference(this.dataset.GetProjection());
      this.wgs84 = new SpatialReference();
      this.wgs84.ImportFromEPSG(4326);
      this.wgs84.SetAxisMappingStrategy(osrConstants.OAMS_TRADITIONAL_GIS_ORDER);
      this.rasterSRS.SetAxisMappingStrategy(osrConstants.OAMS_TRADITIONAL_GIS_ORDER);

      this.toRasterCrs = CoordinateTransformation.CreateCoordinateTransformation(this.wgs84, this.rasterSRS);
      this.rasterExtent = ZonalStatsUtil.createRasterExtentGeometry(this.dataset);
    }

    @Override
    public void close() {
      if (rasterExtent != null) rasterExtent.delete();
      if (toRasterCrs != null) toRasterCrs.delete();
      if (wgs84 != null) wgs84.delete();
      if (rasterSRS != null) rasterSRS.delete();
      if (dataset != null) dataset.delete();
    }
  }

  /**
   * Computes zonal stats for a single location and returns the entity to persist.
   * Returns Optional.empty() when the location should not be persisted (missing/invalid
   * geometry, no intersection with the raster, or zero valid pixels within the polygon).
   */
  private Optional<RasterLocationZonalStats> calculateSingleLocation(String rasterId, Location location,
      Dataset dataset, SpatialReference rasterSRS, CoordinateTransformation toRasterCrs,
      Geometry rasterExtent, String tag) throws Exception {

    if (location == null || location.getGeometry() == null) {
      log.debug("Location {} has no geometry. Skipping.",
          location != null ? location.getIdentifier() : null);
      return Optional.empty();
    }

    Geometry polygon = null;
    try {
      String geoJson = objectMapper.writeValueAsString(location.getGeometry());
      polygon = ZonalStatsUtil.createGeometry(geoJson);
      if (polygon == null) {
        log.warn("Could not create geometry for location {} ({}). Skipping.",
            location.getIdentifier(), location.getName());
        return Optional.empty();
      }

      polygon.Transform(toRasterCrs);

      if (!ZonalStatsUtil.intersects(polygon, rasterExtent)) {
        log.info("Location {} ({}) does not intersect raster extent. Skipping.",
            location.getIdentifier(), location.getName());
        return Optional.empty();
      }

      ZonalStatistics stats = ZonalStatsUtil.computeZonalStats(dataset, rasterSRS, polygon, 1);

      if (stats.getPixelCount() <= 0) {
        log.info("Location {} ({}) has no valid (non-nodata) pixels within the raster. Skipping save.",
            location.getIdentifier(), location.getName());
        return Optional.empty();
      }

      Optional<RasterLocationZonalStats> existing = rasterLocationZonalStatsRepository
          .findByRasterIdAndLocation_Identifier(rasterId, location.getIdentifier());

      RasterLocationZonalStats zonalStats = existing.orElseGet(() -> {
        RasterLocationZonalStats z = RasterLocationZonalStats.builder()
            .rasterId(rasterId)
            .location(location)
            .build();
        z.setEntityStatus(EntityStatus.ACTIVE);
        return z;
      });

      zonalStats.setTag(tag);
      zonalStats.setPixelCount(stats.getPixelCount());
      zonalStats.setMin(stats.getMin());
      zonalStats.setMax(stats.getMax());
      zonalStats.setSum(stats.getSum());
      zonalStats.setMean(stats.getMean());

      return Optional.of(zonalStats);
    } finally {
      if (polygon != null) {
        polygon.delete();
      }
    }
  }

  public boolean shouldSkipGeographicLevel(Location location, List<String> skipGeographicLevels) {
    if (skipGeographicLevels == null || skipGeographicLevels.isEmpty() || location.getGeographicLevel() == null) {
      return false;
    }
    String levelName = location.getGeographicLevel().getName();
    String levelTitle = location.getGeographicLevel().getTitle();
    return skipGeographicLevels.stream().anyMatch(level ->
        (levelName != null && levelName.equalsIgnoreCase(level)) ||
            (levelTitle != null && levelTitle.equalsIgnoreCase(level))
    );
  }
}