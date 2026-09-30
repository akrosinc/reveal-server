package com.revealprecision.revealserver.raster;

import java.io.File;
import java.util.Arrays;
import java.util.Vector;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.WarpOptions;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Preprocessing step: reprojects the source raster into EPSG:3857
 * and writes it as a Cloud-Optimized GeoTIFF (internal tiling + built-in overviews).
 * All subsequent per-tile reads ({@link GdalTileReader}) are fast windowed reads.
 */
public final class CogBuilder {

    private static final Logger log = LoggerFactory.getLogger(CogBuilder.class);

    private final String resampleAlgorithm;
    private final String compression;
    private final int blockSize;

    public CogBuilder(String resampleAlgorithm, String compression, int blockSize) {
        this.resampleAlgorithm = resampleAlgorithm != null ? resampleAlgorithm : "near";
        this.compression = compression != null ? compression : "DEFLATE";
        this.blockSize = blockSize > 0 ? blockSize : 512;
    }

    /** Nearest-neighbor resampling, DEFLATE compression, 512px blocks — defaults for categorical/numeric rasters. */
    public CogBuilder() {
        this("near", "DEFLATE", 512);
    }

    public void build(String srcPath, String dstPath) {
        GdalBootstrap.init();
        log.info("Building Web Mercator COG: {} -> {} (resample={}, compress={}, blocksize={})",
                srcPath, dstPath, resampleAlgorithm, compression, blockSize);

        File dstFile = new File(dstPath);
        File parentDir = dstFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        Dataset src = gdal.Open(srcPath, gdalconstConstants.GA_ReadOnly);
        if (src == null) {
            throw new IllegalStateException("Failed to open source raster: " + srcPath + " — " + gdal.GetLastErrorMsg());
        }

        WarpOptions opts = null;
        Dataset result = null;
        try {
            Vector<String> warpArgs = new Vector<>(Arrays.asList(
                    "-t_srs", "EPSG:3857",
                    "-r", resampleAlgorithm,
                    "-of", "COG",
                    "-co", "COMPRESS=" + compression,
                    "-co", "BLOCKSIZE=" + blockSize,
                    "-co", "OVERVIEWS=AUTO",
                    "-co", "OVERVIEW_RESAMPLING=" + resampleAlgorithm,
                    "-multi",
                    "-wo", "NUM_THREADS=ALL_CPUS"
            ));

            opts = new WarpOptions(warpArgs);
            result = gdal.Warp(dstPath, new Dataset[]{src}, opts);

            if (result == null) {
                throw new IllegalStateException("gdal.Warp failed to create COG: " + gdal.GetLastErrorMsg());
            }
            log.info("COG build complete: {}", dstPath);
        } catch (Exception e) {
            log.error("Failed to build COG {}: {}", dstPath, e.getMessage(), e);
            throw e;
        } finally {
            if (result != null) {
                result.delete();
            }
            if (opts != null) {
                opts.delete();
            }
            src.delete();
        }
    }

    /** Validates that overviews are actually present — a missing-overview COG degrades tile-read performance. */
    public boolean validate(String cogPath) {
        GdalBootstrap.init();
        Dataset ds = gdal.Open(cogPath, gdalconstConstants.GA_ReadOnly);
        if (ds == null) {
            throw new IllegalStateException("Cannot open COG for validation: " + cogPath);
        }
        try {
            int overviewCount = ds.GetRasterBand(1).GetOverviewCount();
            if (overviewCount <= 0) {
                log.warn("COG {} has NO overviews — low-zoom tile reads will decode full resolution. Check the warp step.", cogPath);
                return false;
            } else {
                log.info("COG {} has {} overview levels.", cogPath, overviewCount);
                return true;
            }
        } finally {
            ds.delete();
        }
    }
}
