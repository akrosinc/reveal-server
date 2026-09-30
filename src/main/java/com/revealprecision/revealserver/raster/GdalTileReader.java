package com.revealprecision.revealserver.raster;

import com.revealprecision.revealserver.model.GeoEnvelope;
import java.io.Closeable;
import java.util.Arrays;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads pixel data for a single output tile from a source raster that is
 * in the target CRS (EPSG:3857). Each read is a windowed decode using overview levels.
 *
 * NOT thread-safe. Each worker thread owns its own instance.
 */
public final class GdalTileReader implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(GdalTileReader.class);

    private final Dataset dataset;
    private final Band band;
    private final double originX;
    private final double originY;
    private final double pixelW;
    private final double pixelH;
    private final int rasterW;
    private final int rasterH;
    private final Double nodataValue;
    private final GeoEnvelope bounds;
    private boolean closed = false;

    public GdalTileReader(String cogPath, int bandIndex) {
        GdalBootstrap.init();
        this.dataset = gdal.Open(cogPath, gdalconstConstants.GA_ReadOnly);
        if (dataset == null) {
            throw new IllegalStateException("Failed to open raster: " + cogPath + " — " + gdal.GetLastErrorMsg());
        }

        double[] gt = new double[6];
        dataset.GetGeoTransform(gt);
        this.originX = gt[0];
        this.pixelW = gt[1];
        this.originY = gt[3];
        this.pixelH = gt[5];

        this.rasterW = dataset.getRasterXSize();
        this.rasterH = dataset.getRasterYSize();
        this.band = dataset.GetRasterBand(bandIndex);
        if (this.band == null) {
            dataset.delete();
            throw new IllegalArgumentException("Raster band " + bandIndex + " does not exist in: " + cogPath);
        }

        Double[] nodataOut = new Double[1];
        band.GetNoDataValue(nodataOut);
        this.nodataValue = nodataOut[0];

        double x0 = originX;
        double y0 = originY;
        double x1 = originX + rasterW * pixelW;
        double y1 = originY + rasterH * pixelH;
        this.bounds = new GeoEnvelope(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1), Math.max(y0, y1));

        log.debug("Opened {}: {}x{} px, nodata={}, bounds={}", cogPath, rasterW, rasterH, nodataValue, bounds);
    }

    public GdalTileReader(String cogPath) {
        this(cogPath, 1);
    }

    public GeoEnvelope getBounds() {
        return bounds;
    }

    public Double getNodataValue() {
        return nodataValue;
    }

    /**
     * Reads and resamples the source data overlapping {@code tileEnv} into a {@code tileSize x tileSize} buffer.
     *
     * @return the sample buffer, or {@code null} if the tile does not overlap the raster bounds.
     */
    public int[] readTile(GeoEnvelope tileEnv, int tileSize) {
        if (closed || !tileEnv.intersects(bounds)) {
            return null;
        }

        // Pixel window in source raster space covering the FULL tile envelope
        double srcXd = (tileEnv.getMinX() - originX) / pixelW;
        double srcYd = (tileEnv.getMaxY() - originY) / pixelH;
        double srcXEndD = (tileEnv.getMaxX() - originX) / pixelW;
        double srcYEndD = (tileEnv.getMinY() - originY) / pixelH;

        int srcX = (int) Math.floor(Math.min(srcXd, srcXEndD));
        int srcY = (int) Math.floor(Math.min(srcYd, srcYEndD));
        int srcXEnd = (int) Math.ceil(Math.max(srcXd, srcXEndD));
        int srcYEnd = (int) Math.ceil(Math.max(srcYd, srcYEndD));

        int srcW = srcXEnd - srcX;
        int srcH = srcYEnd - srcY;
        if (srcW <= 0 || srcH <= 0) {
            return null;
        }

        // Clip the window to the raster's actual pixel bounds
        int clippedX = Math.max(srcX, 0);
        int clippedY = Math.max(srcY, 0);
        int clippedXEnd = Math.min(srcX + srcW, rasterW);
        int clippedYEnd = Math.min(srcY + srcH, rasterH);
        int clippedW = clippedXEnd - clippedX;
        int clippedH = clippedYEnd - clippedY;
        if (clippedW <= 0 || clippedH <= 0) {
            return null;
        }

        // Map the clipped source sub-window to its corresponding destination sub-rectangle
        int destX = (int) Math.round((clippedX - srcX) / (double) srcW * tileSize);
        int destY = (int) Math.round((clippedY - srcY) / (double) srcH * tileSize);
        int destXEnd = (int) Math.round((clippedXEnd - srcX) / (double) srcW * tileSize);
        int destYEnd = (int) Math.round((clippedYEnd - srcY) / (double) srcH * tileSize);
        int destW = Math.max(1, Math.min(tileSize, destXEnd) - destX);
        int destH = Math.max(1, Math.min(tileSize, destYEnd) - destY);
        if (destX < 0 || destY < 0 || destX >= tileSize || destY >= tileSize) {
            return null;
        }

        int fillValue = nodataValue != null ? nodataValue.intValue() : 0;
        int[] fullBuffer = new int[tileSize * tileSize];
        Arrays.fill(fullBuffer, fillValue);

        // Fast path: clipped window fills the whole tile buffer exactly.
        if (destX == 0 && destY == 0 && destW == tileSize && destH == tileSize) {
            int err = band.ReadRaster(clippedX, clippedY, clippedW, clippedH, tileSize, tileSize,
                    gdalconstConstants.GDT_Int32, fullBuffer);
            if (err != gdalconstConstants.CE_None) {
                throw new RuntimeException("ReadRaster failed: " + gdal.GetLastErrorMsg());
            }
            return fullBuffer;
        }

        // General path: read into a sub-buffer, then splice into the full tile buffer.
        int[] subBuffer = new int[destW * destH];
        int err = band.ReadRaster(clippedX, clippedY, clippedW, clippedH, destW, destH,
                gdalconstConstants.GDT_Int32, subBuffer);
        if (err != gdalconstConstants.CE_None) {
            throw new RuntimeException("ReadRaster failed: " + gdal.GetLastErrorMsg());
        }

        for (int row = 0; row < destH; row++) {
            System.arraycopy(subBuffer, row * destW, fullBuffer, (destY + row) * tileSize + destX, destW);
        }
        return fullBuffer;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            if (dataset != null) {
                dataset.delete();
            }
        }
    }
}
