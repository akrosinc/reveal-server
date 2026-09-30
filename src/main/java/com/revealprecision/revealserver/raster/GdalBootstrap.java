package com.revealprecision.revealserver.raster;

import lombok.extern.slf4j.Slf4j;
import org.gdal.gdal.gdal;

/**
 * One-time GDAL native library initialization. Must be called before any
 * other GDAL API usage. Safe to call multiple times; only initializes once.
 */
@Slf4j
public final class GdalBootstrap {

    private static volatile boolean initialized = false;

    private GdalBootstrap() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }

        gdal.AllRegister();

        // Let GDAL use all cores for internal operations (warp, overview building)
        gdal.SetConfigOption("GDAL_NUM_THREADS", "ALL_CPUS");

        // Block cache size in MB. Tune to available RAM; bigger helps when many
        // threads are re-reading overlapping regions of the same COG.
        gdal.SetConfigOption("GDAL_CACHEMAX", System.getProperty("raster2mvt.gdalCacheMb", "1024"));

        // Speeds up repeated remote/local reads of the same dataset across threads.
        gdal.SetConfigOption("VSI_CACHE", "TRUE");
        gdal.SetConfigOption("VSI_CACHE_SIZE", "25000000");

        // Don't let GDAL print to stderr on recoverable errors; we handle errors explicitly.
        gdal.PushErrorHandler("CPLQuietErrorHandler");

        initialized = true;
        log.info("GDAL initialized: version={}", gdal.VersionInfo("RELEASE_NAME"));
    }

    public static boolean isInitialized() {
        return initialized;
    }
}