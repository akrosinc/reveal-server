package com.revealprecision.revealserver.raster;

import com.revealprecision.revealserver.model.ZonalStatistics;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Feature;
import org.gdal.ogr.FeatureDefn;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;
import org.gdal.ogr.ogrConstants;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osrConstants;

public final class ZonalStatsUtil {

  private ZonalStatsUtil() {
  }

  /**
   * Computes zonal statistics for band 1, given a polygon in WGS84 (EPSG:4326)
   * as WKT, e.g. {@code "POLYGON((-1.7 6.5, -1.5 6.5, -1.5 6.7, -1.7 6.7, -1.7 6.5))"}.
   *
   * Opens and closes the raster for you - convenient for one-off calls.
   */
  public static ZonalStatistics computeZonalStats(String rasterPath, String polygonWktWgs84) {
    return computeZonalStats(rasterPath, polygonWktWgs84, 1);
  }

  public static ZonalStatistics computeZonalStats(String rasterPath, String polygonWktWgs84, int bandIndex) {
    GdalBootstrap.init();
    Dataset dataset = gdal.Open(rasterPath, gdalconstConstants.GA_ReadOnly);
    if (dataset == null) {
      throw new IllegalArgumentException(
          "Could not open raster: " + rasterPath + " - " + gdal.GetLastErrorMsg());
    }
    try {
      return computeZonalStats(dataset, polygonWktWgs84, bandIndex);
    } finally {
      dataset.delete();
    }
  }

  /**
   * Computes zonal statistics for an already-open raster.
   *
   * @param dataset         an open GDAL {@link Dataset}
   * @param polygonWktWgs84 polygon in WGS84 (EPSG:4326) as WKT
   * @param bandIndex       1-based band index to compute stats for
   */
  public static ZonalStatistics computeZonalStats(Dataset dataset, String polygonWktWgs84, int bandIndex) {
    GdalBootstrap.init();
    SpatialReference rasterSRS = new SpatialReference(dataset.GetProjection());

    SpatialReference wgs84 = new SpatialReference();
    wgs84.ImportFromEPSG(4326);
    wgs84.SetAxisMappingStrategy(osrConstants.OAMS_TRADITIONAL_GIS_ORDER);
    rasterSRS.SetAxisMappingStrategy(osrConstants.OAMS_TRADITIONAL_GIS_ORDER);

    Geometry polygon = createGeometry(polygonWktWgs84);
    if (polygon == null) {
      rasterSRS.delete();
      wgs84.delete();
      return new ZonalStatistics();
    }
    CoordinateTransformation toRasterCrs =
        CoordinateTransformation.CreateCoordinateTransformation(wgs84, rasterSRS);
    polygon.Transform(toRasterCrs);

    try {
      return computeZonalStats(dataset, rasterSRS, polygon, bandIndex);
    } finally {
      polygon.delete();
      toRasterCrs.delete();
      wgs84.delete();
      rasterSRS.delete();
    }
  }

  /**
   * Computes zonal statistics for an already-open raster with polygon already transformed into raster's SRS.
   *
   * <p>Note: when the polygon contains zero valid (non-nodata) pixels, the returned
   * {@link ZonalStatistics} will have {@code pixelCount == 0} and min/max/sum/mean left at
   * their model defaults. Callers must check {@code pixelCount} before trusting min/max/mean -
   * do not persist those fields when pixelCount is 0.</p>
   */
  public static ZonalStatistics computeZonalStats(Dataset dataset, SpatialReference rasterSRS,
      Geometry polygonInRasterCrs, int bandIndex) {

    if (bandIndex < 1 || bandIndex > dataset.GetRasterCount()) {
      throw new IllegalArgumentException(
          "Invalid band index " + bandIndex + " for dataset with " + dataset.GetRasterCount() + " band(s)");
    }

    // 2. Figure out the pixel window covering the polygon's bounding box.
    double[] geoTransform = dataset.GetGeoTransform();
    double[] invTransform = new double[6];
    gdal.InvGeoTransform(geoTransform, invTransform);

    double[] env = new double[4]; // minX, maxX, minY, maxY
    polygonInRasterCrs.GetEnvelope(env);

    int[] winMin = xyToPixel(invTransform, env[0], env[3]); // top-left
    int[] winMax = xyToPixel(invTransform, env[1], env[2]); // bottom-right

    int xoff = Math.max(0, Math.min(winMin[0], winMax[0]));
    int yoff = Math.max(0, Math.min(winMin[1], winMax[1]));
    int xEnd = Math.min(dataset.GetRasterXSize(), Math.max(winMin[0], winMax[0]) + 1);
    int yEnd = Math.min(dataset.GetRasterYSize(), Math.max(winMin[1], winMax[1]) + 1);
    int xsize = xEnd - xoff;
    int ysize = yEnd - yoff;

    if (xsize <= 0 || ysize <= 0) {
      return new ZonalStatistics();
    }

    // 3. Rasterize the polygon into an in-memory byte mask aligned with that window.
    double[] windowGeoTransform = {
        geoTransform[0] + xoff * geoTransform[1] + yoff * geoTransform[2],
        geoTransform[1], geoTransform[2],
        geoTransform[3] + xoff * geoTransform[4] + yoff * geoTransform[5],
        geoTransform[4], geoTransform[5]
    };

    Driver memRasterDriver = gdal.GetDriverByName("MEM");
    Dataset maskDs = memRasterDriver.Create("", xsize, ysize, 1, gdalconstConstants.GDT_Byte);
    DataSource memVectorDs = null;
    Feature feature = null;
    try {
      maskDs.SetGeoTransform(windowGeoTransform);
      maskDs.SetProjection(rasterSRS.ExportToWkt());

      memVectorDs = ogr.GetDriverByName("Memory").CreateDataSource("mask");
      Layer layer = memVectorDs.CreateLayer("polygon", rasterSRS, ogrConstants.wkbPolygon);
      FeatureDefn featureDefn = layer.GetLayerDefn();
      feature = new Feature(featureDefn);
      feature.SetGeometry(polygonInRasterCrs);
      layer.CreateFeature(feature);

      gdal.RasterizeLayer(maskDs, new int[]{1}, layer, new double[]{1.0});

      // Byte mask instead of double: 8x less memory, faster read/scan.
      byte[] maskBuffer = new byte[xsize * ysize];
      maskDs.GetRasterBand(1).ReadRaster(0, 0, xsize, ysize, maskBuffer);

      // 4. Read the matching window of actual raster data and accumulate stats
      //    only where the mask says "inside the polygon".
      Band band = dataset.GetRasterBand(bandIndex);
      Double[] nodataBox = new Double[1];
      band.GetNoDataValue(nodataBox);
      Double nodata = nodataBox[0];

      double[] dataBuffer = new double[xsize * ysize];
      band.ReadRaster(xoff, yoff, xsize, ysize, dataBuffer);

      long count = 0;
      double sum = 0;
      double min = Double.MAX_VALUE;
      double max = -Double.MAX_VALUE;

      for (int i = 0; i < dataBuffer.length; i++) {
        if (maskBuffer[i] == 0) {
          continue; // outside the polygon
        }
        double value = dataBuffer[i];
        if (nodata != null && value == nodata) {
          continue; // inside the polygon, but nodata
        }
        count++;
        sum += value;
        if (value < min) min = value;
        if (value > max) max = value;
      }

      ZonalStatistics stats = new ZonalStatistics();
      stats.setPixelCount(count);
      if (count > 0) {
        stats.setSum(sum);
        stats.setMean(sum / count);
        stats.setMin(min);
        stats.setMax(max);
      }
      return stats;
    } finally {
      if (feature != null) {
        feature.delete();
      }
      if (memVectorDs != null) {
        memVectorDs.delete();
      }
      maskDs.delete();
    }
  }

  /**
   * Creates an OGR Geometry from either GeoJSON or WKT string representation.
   */
  public static Geometry createGeometry(String wktOrGeoJson) {
    if (wktOrGeoJson == null || wktOrGeoJson.isBlank()) {
      return null;
    }
    String trimmed = wktOrGeoJson.trim();
    if (trimmed.startsWith("{")) {
      return Geometry.CreateFromJson(trimmed);
    } else {
      return Geometry.CreateFromWkt(trimmed);
    }
  }

  /**
   * Checks if a polygon in raster CRS intersects with the raster extent geometry.
   */
  public static boolean intersects(Geometry polygonInRasterCrs, Geometry rasterExtent) {
    if (polygonInRasterCrs == null || rasterExtent == null) {
      return false;
    }
    double[] polyEnv = new double[4];
    polygonInRasterCrs.GetEnvelope(polyEnv);

    double[] rasterEnv = new double[4];
    rasterExtent.GetEnvelope(rasterEnv);

    if (polyEnv[1] < rasterEnv[0] || polyEnv[0] > rasterEnv[1] || polyEnv[3] < rasterEnv[2]
        || polyEnv[2] > rasterEnv[3]) {
      return false;
    }
    return polygonInRasterCrs.Intersect(rasterExtent);
  }

  /**
   * Creates a polygon geometry representing the full extent bounding box of the dataset in raster CRS.
   */
  public static Geometry createRasterExtentGeometry(Dataset dataset) {
    double[] geoTransform = dataset.GetGeoTransform();
    int xSize = dataset.GetRasterXSize();
    int ySize = dataset.GetRasterYSize();

    double x0 = geoTransform[0];
    double y0 = geoTransform[3];
    double x1 = geoTransform[0] + xSize * geoTransform[1];
    double y1 = geoTransform[3] + xSize * geoTransform[4];
    double x2 = geoTransform[0] + xSize * geoTransform[1] + ySize * geoTransform[2];
    double y2 = geoTransform[3] + xSize * geoTransform[4] + ySize * geoTransform[5];
    double x3 = geoTransform[0] + ySize * geoTransform[2];
    double y3 = geoTransform[3] + ySize * geoTransform[5];

    double minX = Math.min(Math.min(x0, x1), Math.min(x2, x3));
    double maxX = Math.max(Math.max(x0, x1), Math.max(x2, x3));
    double minY = Math.min(Math.min(y0, y1), Math.min(y2, y3));
    double maxY = Math.max(Math.max(y0, y1), Math.max(y2, y3));

    Geometry rasterExtent = new Geometry(ogrConstants.wkbPolygon);
    Geometry ring = new Geometry(ogrConstants.wkbLinearRing);
    ring.AddPoint_2D(minX, minY);
    ring.AddPoint_2D(maxX, minY);
    ring.AddPoint_2D(maxX, maxY);
    ring.AddPoint_2D(minX, maxY);
    ring.AddPoint_2D(minX, minY);
    rasterExtent.AddGeometry(ring);
    return rasterExtent;
  }

  /**
   * Converts CRS x/y (already in the raster's own CRS) into pixel/line coordinates.
   */
  private static int[] xyToPixel(double[] invTransform, double x, double y) {
    double pixelXd = invTransform[0] + x * invTransform[1] + y * invTransform[2];
    double pixelYd = invTransform[3] + x * invTransform[4] + y * invTransform[5];
    return new int[]{(int) Math.floor(pixelXd), (int) Math.floor(pixelYd)};
  }
}