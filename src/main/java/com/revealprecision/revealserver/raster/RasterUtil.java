package com.revealprecision.revealserver.raster;


import com.revealprecision.revealserver.exceptions.InvalidRasterEventException;
import com.revealprecision.revealserver.exceptions.RasterProcessingException;
import com.revealprecision.revealserver.model.GeoEnvelope;
import com.revealprecision.revealserver.model.TileRange;
import com.revealprecision.revealserver.props.RasterIngestionProperties;
import com.revealprecision.revealserver.service.models.RasterLocationPaths;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;

public class RasterUtil {

  public static final double WORLD_HALF_SIZE = 20037508.342789244;
  private static final double EPSILON = 1e-9;

  /** Number of tiles per axis at a given zoom level (2^z). */
  public static int tilesAcross(int z) {
    return 1 << z;
  }

  public static GeoEnvelope tileEnvelope(int z, int x, int y) {
    int tilesAcross = tilesAcross(z);
    double tileSize = (2.0 * WORLD_HALF_SIZE) / tilesAcross;

    double minX = -WORLD_HALF_SIZE + x * tileSize;
    double maxX = minX + tileSize;
    double maxY = WORLD_HALF_SIZE - y * tileSize;
    double minY = maxY - tileSize;

    return new GeoEnvelope(minX, minY, maxX, maxY);
  }

  /**
   * Calculates the bounding range of tile coordinates (x, y) at zoom level {@code z}
   * that intersect the given {@code bounds}.
   *
   * @param z zoom level
   * @param bounds EPSG:3857 envelope
   * @return intersecting TileRange, or {@code null} if no intersection with world bounds
   */
  public static TileRange envelopeToTileRange(int z, GeoEnvelope bounds) {
    int tilesAcross = tilesAcross(z);
    double tileSize = (2.0 * WORLD_HALF_SIZE) / tilesAcross;

    if (bounds.getMaxX() < -WORLD_HALF_SIZE
        || bounds.getMinX() > WORLD_HALF_SIZE
        || bounds.getMaxY() < -WORLD_HALF_SIZE
        || bounds.getMinY() > WORLD_HALF_SIZE) {
      return null;
    }

    int minX = (int) Math.floor(
        (bounds.getMinX() + WORLD_HALF_SIZE) / tileSize);

    int maxX = (int) Math.floor(
        (bounds.getMaxX() - EPSILON + WORLD_HALF_SIZE) / tileSize);

    int minY = (int) Math.floor(
        (WORLD_HALF_SIZE - bounds.getMaxY()) / tileSize);

    int maxY = (int) Math.floor(
        (WORLD_HALF_SIZE - (bounds.getMinY() + EPSILON)) / tileSize);

    minX = Math.max(0, Math.min(tilesAcross - 1, minX));
    maxX = Math.max(0, Math.min(tilesAcross - 1, maxX));
    minY = Math.max(0, Math.min(tilesAcross - 1, minY));
    maxY = Math.max(0, Math.min(tilesAcross - 1, maxY));

    if (minX > maxX || minY > maxY) {
      return null;
    }

    return new TileRange(minX, maxX, minY, maxY);
  }

  public static String buildCogFilePath(String rasterFilePath) {
    File rasterFile = new File(rasterFilePath);
    String parent = rasterFile.getParent();
    String name = rasterFile.getName();
    int dotIndex = name.lastIndexOf('.');
    String baseName = dotIndex != -1 ? name.substring(0, dotIndex) : name;
    String extension = dotIndex != -1 ? name.substring(dotIndex) : ".tif";
    String cogFileName = baseName + "_cog" + extension;
    return parent != null ? new File(parent, cogFileName).getAbsolutePath() : cogFileName;
  }

  public static RasterLocationPaths validateAndResolvePaths(String rasterId, RasterIngestionProperties properties) {
    if (rasterId == null || rasterId.isBlank() || rasterId.contains("..") || rasterId.contains("/")
        || rasterId.contains("\\")) {
      throw new InvalidRasterEventException("Invalid taskIdentifier: " + rasterId);
    }

    Path basePath = Paths.get(properties.getBasePath()).toAbsolutePath().normalize();
    Path rasterFolder = basePath.resolve(rasterId).normalize();

    if (!rasterFolder.startsWith(basePath)) {
      throw new InvalidRasterEventException("Access denied: path escapes base directory");
    }

    File folder = rasterFolder.toFile();
    if (!folder.exists() || !folder.isDirectory()) {
      throw new InvalidRasterEventException("Raster folder not found: " + rasterFolder);
    }

    List<File> rasterFiles = Arrays.stream(folder.listFiles())
        .filter(f -> f.isFile() && isRasterFile(f.getName()))
        .collect(Collectors.toList());

    if (rasterFiles.isEmpty()) {
      throw new InvalidRasterEventException("No raster file found in " + rasterFolder);
    }
    if (rasterFiles.size() > 2) {
      throw new InvalidRasterEventException("Multiple raster files found in " + rasterFolder);
    }
    File rasterFile = null;
    String cogPath = null;
    if (rasterFiles.size() == 2) {
      File original = rasterFiles.stream()
          .filter(f -> !f.getName().contains("_cog"))
          .findFirst()
          .orElse(null);
      File cog = rasterFiles.stream()
          .filter(f -> f.getName().contains("_cog"))
          .findFirst()
          .orElse(null);

      if (original == null || cog == null) {
        throw new InvalidRasterEventException("Multiple raster files found in " + rasterFolder);
      }
      rasterFile = original;
      cogPath = cog.getAbsolutePath();
    } else {
      rasterFile = rasterFiles.get(0);
      if (rasterFile.getName().contains("_cog")) {
        cogPath = rasterFile.getAbsolutePath();
      } else {
        cogPath = buildCogFilePath(rasterFile.getAbsolutePath());
      }
    }

    if (!rasterFile.canRead() || rasterFile.length() == 0) {
      throw new InvalidRasterEventException(
          "Raster file is unreadable or empty: " + rasterFile.getAbsolutePath());
    }

    Path tilesPath = rasterFolder.resolve(properties.getTilesSubdirName());
    try {
      Files.createDirectories(tilesPath);
    } catch (IOException e) {
      throw new RasterProcessingException("Failed to create tiles directory: " + tilesPath, e);
    }

    return new RasterLocationPaths(rasterFile.getAbsolutePath(), tilesPath.toString(), cogPath);
  }

  public static boolean isRasterFile(String filename) {
    String lower = filename.toLowerCase();
    return lower.endsWith(".tif") || lower.endsWith(".tiff") || lower.endsWith(".img") || lower
        .endsWith(".vrt");
  }

  public static GeoEnvelope getRasterExtent(String rasterFilePath) {
    GdalBootstrap.init();

    Dataset ds = gdal.Open(rasterFilePath, gdalconstConstants.GA_ReadOnly);
    if (ds == null) {
      throw new InvalidRasterEventException(
          "Could not open raster: " + rasterFilePath + " - " + gdal.GetLastErrorMsg());
    }

    try {
      double[] e = new double[4];
      ds.GetExtent(e);

      // ASSUMPTION: order is {minX, maxX, minY, maxY} (OGR-style).
      // Verify against a known raster; if it is {minX, minY, maxX, maxY},
      // change the constructor arguments below.
      return new GeoEnvelope(e[0], e[2], e[1], e[3]);
    } finally {
      ds.delete();
    }
  }
}
