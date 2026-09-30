package com.revealprecision.revealserver.raster;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osrConstants;

/**
 * Utility for querying raster pixel values at geographic coordinates (WGS84 lat/lon).
 * Ensures all GDAL/OSR native objects are properly deleted to prevent memory leaks.
 */
public final class RasterValueUtil {

    private RasterValueUtil() {
    }

    public static double[] getValueAtLatLng(String rasterPath, double lat, double lon) {
        GdalBootstrap.init();
        Dataset dataset = gdal.Open(rasterPath, gdalconstConstants.GA_ReadOnly);
        if (dataset == null) {
            throw new IllegalArgumentException(
                    "Could not open raster: " + rasterPath + " - " + gdal.GetLastErrorMsg());
        }
        try {
            return getValueAtLatLng(dataset, lat, lon);
        } finally {
            dataset.delete();
        }
    }

    /**
     * Looks up the value at {@code lat}/{@code lon} in an already-open raster.
     *
     * @param dataset an open GDAL {@link Dataset}
     * @param lat     latitude in WGS84 (EPSG:4326)
     * @param lon     longitude in WGS84 (EPSG:4326)
     * @return one value per band, or {@code null} if the point falls outside the raster's extent
     */
    public static double[] getValueAtLatLng(Dataset dataset, double lat, double lon) {
        if (dataset == null) {
            return null;
        }
        int[] pixel = latLngToPixel(dataset, lat, lon);
        if (pixel == null) {
            return null;
        }
        return readPixel(dataset, pixel[0], pixel[1]);
    }

    /**
     * Converts a lat/lon point into pixel/line coordinates for the given raster.
     *
     * @return {@code [pixelX, pixelY]}, or {@code null} if outside the raster's extent
     */
    public static int[] latLngToPixel(Dataset dataset, double lat, double lon) {
        if (dataset == null) {
            return null;
        }

        SpatialReference rasterSRS = null;
        SpatialReference wgs84 = null;
        CoordinateTransformation transform = null;

        try {
            String proj = dataset.GetProjection();
            rasterSRS = new SpatialReference(proj);

            wgs84 = new SpatialReference();
            wgs84.ImportFromEPSG(4326);

            wgs84.SetAxisMappingStrategy(osrConstants.OAMS_TRADITIONAL_GIS_ORDER);
            rasterSRS.SetAxisMappingStrategy(osrConstants.OAMS_TRADITIONAL_GIS_ORDER);

            transform = CoordinateTransformation.CreateCoordinateTransformation(wgs84, rasterSRS);
            if (transform == null) {
                return null;
            }

            double[] transformed = transform.TransformPoint(lon, lat);
            double rasterX = transformed[0];
            double rasterY = transformed[1];

            double[] geoTransform = dataset.GetGeoTransform();
            double[] invTransform = new double[6];
            int invertible = gdal.InvGeoTransform(geoTransform, invTransform);
            if (invertible == 0) {
                return null;
            }

            double pixelXd = invTransform[0] + rasterX * invTransform[1] + rasterY * invTransform[2];
            double pixelYd = invTransform[3] + rasterX * invTransform[4] + rasterY * invTransform[5];

            int pixelX = (int) Math.floor(pixelXd);
            int pixelY = (int) Math.floor(pixelYd);

            if (pixelX < 0 || pixelY < 0
                    || pixelX >= dataset.GetRasterXSize()
                    || pixelY >= dataset.GetRasterYSize()) {
                return null;
            }

            return new int[]{pixelX, pixelY};
        } finally {
            if (transform != null) transform.delete();
            if (wgs84 != null) wgs84.delete();
            if (rasterSRS != null) rasterSRS.delete();
        }
    }

    /** Reads one value per band at the given pixel/line coordinates. */
    public static double[] readPixel(Dataset dataset, int pixelX, int pixelY) {
        if (dataset == null) {
            return null;
        }
        int bandCount = dataset.GetRasterCount();
        double[] values = new double[bandCount];
        double[] buffer = new double[1];

        for (int b = 1; b <= bandCount; b++) {
            Band band = dataset.GetRasterBand(b);
            if (band != null) {
                int err = band.ReadRaster(pixelX, pixelY, 1, 1, buffer);
                if (err == gdalconstConstants.CE_None) {
                    values[b - 1] = buffer[0];
                }
            }
        }

        return values;
    }

    /**
     * Returns the nodata value for a given band, or {@code null} if the band has none set.
     */
    public static Double getNoDataValue(Dataset dataset, int bandIndex) {
        if (dataset == null) {
            return null;
        }
        Band band = dataset.GetRasterBand(bandIndex);
        if (band == null) {
            return null;
        }
        Double[] nodata = new Double[1];
        band.GetNoDataValue(nodata);
        return nodata[0];
    }
}
