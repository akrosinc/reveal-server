package com.revealprecision.revealserver.raster;

import com.revealprecision.revealserver.model.GeoEnvelope;
import com.revealprecision.revealserver.model.TileRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Orchestrates generation of a full MVT tile pyramid from a source raster
 * that has already been preprocessed into a Web Mercator COG (see {@link CogBuilder}).
 *
 * For each (z, x, y) tile in the requested zoom range:
 *   1. Reads windowed pixels via {@link GdalTileReader}.
 *   2. Run-length encodes same-value pixel runs into rectangles via {@link PixelRectExtractor}.
 *   3. Encodes those rectangles as an MVT layer via {@link RasterMvtEncoder}.
 *   4. Writes the result as a {z}/{x}/{y}.mvt file via {@link TileFileWriter}.
 */
public final class TiffProcessor {

    private static final Logger log = LoggerFactory.getLogger(TiffProcessor.class);

    private final Config config;
    private final RasterMvtEncoder encoder;
    private final Set<GdalTileReader> openReaders = ConcurrentHashMap.newKeySet();
    private final ThreadLocal<GdalTileReader> readerThreadLocal;

    private final AtomicLong tilesConsidered = new AtomicLong();
    private final AtomicLong tilesWritten = new AtomicLong();
    private final AtomicLong tilesSkippedEmpty = new AtomicLong();
    private final AtomicLong tilesFailed = new AtomicLong();

    public TiffProcessor(Config config) {
        this.config = config;
        this.readerThreadLocal = ThreadLocal.withInitial(() -> {
            GdalTileReader reader = new GdalTileReader(config.cogPath(), config.bandIndex());
            openReaders.add(reader);
            return reader;
        });
        this.encoder = new RasterMvtEncoder(
            config.layerName(),
            config.valueAttribute(),
            config.tileExtent());
    }

    /**
     * Runs the full tiling job synchronously (blocks until complete or failed).
     *
     * @throws TilingException if any tile permanently fails after the configured retries
     */
    public Result run() throws TilingException {
        GdalBootstrap.init();

        long startTime = System.currentTimeMillis();

        log.info(
            "Starting tile generation: zoom {}-{}, threads={}, output={}",
            config.minZoom(),
            config.maxZoom(),
            config.threadCount(),
            config.outputDir());

        GeoEnvelope rasterBounds;
        Double nodata;

        try (GdalTileReader probe =
            new GdalTileReader(config.cogPath(), config.bandIndex())) {

            rasterBounds = probe.getBounds();
            nodata = probe.getNodataValue();

        } catch (Exception e) {
            throw new TilingException(
                "Failed to inspect raster bounds from: " + config.cogPath(), e);
        }

        Integer nodataInt =
            nodata != null ? nodata.intValue() : config.nodataOverride();

        log.info("Raster bounds: {}, nodata value: {}", rasterBounds, nodataInt);

        ExecutorService pool =
            Executors.newFixedThreadPool(config.threadCount());

        TileFileWriter writer = null;

        try {
            writer = new TileFileWriter(
                Path.of(config.outputDir()),
                config.gzip());

            double[] lonLat = webMercatorBoundsToLonLat(rasterBounds);

            writer.writeMetadata(
                config.layerName(),
                "Vector tiles generated from raster " + config.cogPath(),
                config.minZoom(),
                config.maxZoom(),
                lonLat,
                config.layerName(),
                config.valueAttribute());

            AtomicReference<Throwable> fatalError =
                new AtomicReference<>();

            int maxPendingTasks =
                Math.max(1000, config.threadCount() * 100);

            Semaphore taskSemaphore =
                new Semaphore(maxPendingTasks);

            for (int z = config.minZoom();
                z <= config.maxZoom();
                z++) {

                if (fatalError.get() != null && config.failFast()) {
                    break;
                }

                TileRange range =
                    RasterUtil.envelopeToTileRange(z, rasterBounds);

                if (range == null) {
                    continue;
                }

                log.info(
                    "Zoom {}: tiling range X=[{}..{}], Y=[{}..{}] ({} candidate tiles)",
                    z,
                    range.getMinX(),
                    range.getMaxX(),
                    range.getMinY(),
                    range.getMaxY(),
                    range.tileCount());

                for (int x = range.getMinX();
                    x <= range.getMaxX();
                    x++) {

                    for (int y = range.getMinY();
                        y <= range.getMaxY();
                        y++) {

                        if (fatalError.get() != null && config.failFast()) {
                            break;
                        }

                        GeoEnvelope tileEnv =
                            RasterUtil.tileEnvelope(z, x, y);

                        if (!tileEnv.intersects(rasterBounds)) {
                            continue;
                        }

                        taskSemaphore.acquire();

                        final int fz = z;
                        final int fx = x;
                        final int fy = y;
                        final TileFileWriter fWriter = writer;

                        pool.submit(() -> {
                            try {
                                if (fatalError.get() == null
                                    || !config.failFast()) {

                                    processTile(
                                        fz,
                                        fx,
                                        fy,
                                        tileEnv,
                                        fWriter,
                                        nodataInt);
                                }

                            } catch (Throwable t) {
                                tilesFailed.incrementAndGet();

                                log.error(
                                    "Tile generation failed for {}/{}/{}",
                                    fz,
                                    fx,
                                    fy,
                                    t);

                                fatalError.compareAndSet(null, t);

                            } finally {
                                taskSemaphore.release();
                            }
                        });
                    }
                }
            }

            // Await all queued tasks completion
            taskSemaphore.acquire(maxPendingTasks);

            pool.shutdown();

            if (!pool.awaitTermination(
                30,
                TimeUnit.MINUTES)) {

                pool.shutdownNow();

                throw new TilingException(
                    "Tiling process timed out waiting for worker pool shutdown",
                    null);
            }

            Throwable failure = fatalError.get();

            if (failure != null
                && (config.failFast()
                || config.failOnAnyError())) {

                throw new TilingException(
                    "One or more tiles failed to generate",
                    failure);
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            pool.shutdownNow();

            throw new TilingException(
                "Interrupted during tiling job",
                e);

        } catch (TilingException e) {

            pool.shutdownNow();
            throw e;

        } catch (Exception e) {

            pool.shutdownNow();

            throw new TilingException(
                "Tile generation failed: " + e.getMessage(),
                e);

        } finally {

            if (writer != null) {
                writer.close();
            }

            closeAllReaders();
        }

        long elapsedMs =
            System.currentTimeMillis() - startTime;

        Result result = new Result(
            tilesConsidered.get(),
            tilesWritten.get(),
            tilesSkippedEmpty.get(),
            tilesFailed.get(),
            elapsedMs);

        log.info("Tile generation complete: {}", result);

        return result;
    }

    private void processTile(
        int z,
        int x,
        int y,
        GeoEnvelope tileEnv,
        TileFileWriter writer,
        Integer nodataInt) {

        tilesConsidered.incrementAndGet();

        try {
            GdalTileReader reader =
                readerThreadLocal.get();

            int[] buffer =
                reader.readTile(
                    tileEnv,
                    config.tileSize());

            if (buffer == null) {
                tilesSkippedEmpty.incrementAndGet();
                return;
            }

            List<PixelRect> rects =
                PixelRectExtractor.extract(
                    buffer,
                    config.tileSize(),
                    config.tileExtent(),
                    nodataInt);

            if (rects.isEmpty()) {
                tilesSkippedEmpty.incrementAndGet();
                return;
            }

            byte[] mvtBytes =
                encoder.encode(rects);

            if (mvtBytes == null
                || mvtBytes.length == 0) {

                tilesSkippedEmpty.incrementAndGet();
                return;
            }

            writer.writeTile(
                z,
                x,
                y,
                mvtBytes);

            long written =
                tilesWritten.incrementAndGet();

            if (written % 1000 == 0) {
                log.info(
                    "Progress: {} tiles written ({} considered, {} skipped empty, {} failed)",
                    written,
                    tilesConsidered.get(),
                    tilesSkippedEmpty.get(),
                    tilesFailed.get());
            }

        } catch (Exception e) {

            throw new RuntimeException(
                "Tile " + z + "/" + x + "/" + y
                    + " processing failed",
                e);
        }
    }

    private void closeAllReaders() {
        for (GdalTileReader reader : openReaders) {
            try {
                reader.close();
            } catch (Exception e) {
                log.warn(
                    "Error closing GdalTileReader",
                    e);
            }
        }

        openReaders.clear();
        readerThreadLocal.remove();
    }

    private static double[] webMercatorBoundsToLonLat(
        GeoEnvelope b) {

        double minLon =
            b.getMinX()
                / RasterUtil.WORLD_HALF_SIZE
                * 180.0;

        double maxLon =
            b.getMaxX()
                / RasterUtil.WORLD_HALF_SIZE
                * 180.0;

        double minLat =
            mercatorYToLat(b.getMinY());

        double maxLat =
            mercatorYToLat(b.getMaxY());

        return new double[]{
            minLon,
            minLat,
            maxLon,
            maxLat
        };
    }

    private static double mercatorYToLat(double y) {
        double n =
            y / RasterUtil.WORLD_HALF_SIZE * Math.PI;

        return Math.toDegrees(
            Math.atan(Math.sinh(n)));
    }

    /**
     * Immutable configuration for a tiling run.
     */
    public static final class Config {

        private final String cogPath;
        private final String outputDir;
        private final int minZoom;
        private final int maxZoom;
        private final int tileSize;
        private final int tileExtent;
        private final int threadCount;
        private final boolean gzip;
        private final int bandIndex;
        private final String layerName;
        private final String valueAttribute;
        private final Integer nodataOverride;
        private final boolean failFast;
        private final boolean failOnAnyError;

        public Config(
            String cogPath,
            String outputDir,
            int minZoom,
            int maxZoom,
            int tileSize,
            int tileExtent,
            int threadCount,
            boolean gzip,
            int bandIndex,
            String layerName,
            String valueAttribute,
            Integer nodataOverride,
            boolean failFast,
            boolean failOnAnyError) {

            this.cogPath = cogPath;
            this.outputDir = outputDir;
            this.minZoom = minZoom;
            this.maxZoom = maxZoom;
            this.tileSize = tileSize;
            this.tileExtent = tileExtent;
            this.threadCount = threadCount;
            this.gzip = gzip;
            this.bandIndex = bandIndex;
            this.layerName = layerName;
            this.valueAttribute = valueAttribute;
            this.nodataOverride = nodataOverride;
            this.failFast = failFast;
            this.failOnAnyError = failOnAnyError;
        }

        public String cogPath() {
            return cogPath;
        }

        public String outputDir() {
            return outputDir;
        }

        public int minZoom() {
            return minZoom;
        }

        public int maxZoom() {
            return maxZoom;
        }

        public int tileSize() {
            return tileSize;
        }

        public int tileExtent() {
            return tileExtent;
        }

        public int threadCount() {
            return threadCount;
        }

        public boolean gzip() {
            return gzip;
        }

        public int bandIndex() {
            return bandIndex;
        }

        public String layerName() {
            return layerName;
        }

        public String valueAttribute() {
            return valueAttribute;
        }

        public Integer nodataOverride() {
            return nodataOverride;
        }

        public boolean failFast() {
            return failFast;
        }

        public boolean failOnAnyError() {
            return failOnAnyError;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {

            private String cogPath;
            private String outputDir;
            private int minZoom = 0;
            private int maxZoom = 14;
            private int tileSize = 256;
            private int tileExtent = 4096;
            private int threadCount =
                Runtime.getRuntime().availableProcessors();
            private boolean gzip = false;
            private int bandIndex = 1;
            private String layerName = "landcover";
            private String valueAttribute = "class";
            private Integer nodataOverride = null;
            private boolean failFast = false;
            private boolean failOnAnyError = false;

            public Builder cogPath(String v) {
                this.cogPath = v;
                return this;
            }

            public Builder outputDir(String v) {
                this.outputDir = v;
                return this;
            }

            public Builder minZoom(int v) {
                this.minZoom = v;
                return this;
            }

            public Builder maxZoom(int v) {
                this.maxZoom = v;
                return this;
            }

            public Builder tileSize(int v) {
                this.tileSize = v;
                return this;
            }

            public Builder tileExtent(int v) {
                this.tileExtent = v;
                return this;
            }

            public Builder threadCount(int v) {
                this.threadCount = v;
                return this;
            }

            public Builder gzip(boolean v) {
                this.gzip = v;
                return this;
            }

            public Builder bandIndex(int v) {
                this.bandIndex = v;
                return this;
            }

            public Builder layerName(String v) {
                this.layerName = v;
                return this;
            }

            public Builder valueAttribute(String v) {
                this.valueAttribute = v;
                return this;
            }

            public Builder nodataOverride(Integer v) {
                this.nodataOverride = v;
                return this;
            }

            public Builder failFast(boolean v) {
                this.failFast = v;
                return this;
            }

            public Builder failOnAnyError(boolean v) {
                this.failOnAnyError = v;
                return this;
            }

            public Config build() {

                Objects.requireNonNull(
                    cogPath,
                    "cogPath is required");

                Objects.requireNonNull(
                    outputDir,
                    "outputDir is required");

                if (minZoom < 0
                    || maxZoom < minZoom) {

                    throw new IllegalArgumentException(
                        "Invalid zoom range: "
                            + minZoom
                            + "-"
                            + maxZoom);
                }

                return new Config(
                    cogPath,
                    outputDir,
                    minZoom,
                    maxZoom,
                    tileSize,
                    tileExtent,
                    threadCount,
                    gzip,
                    bandIndex,
                    layerName,
                    valueAttribute,
                    nodataOverride,
                    failFast,
                    failOnAnyError);
            }
        }
    }

    /**
     * Summary statistics for a completed tiling run.
     */
    public static final class Result {

        private final long tilesConsidered;
        private final long tilesWritten;
        private final long tilesSkippedEmpty;
        private final long tilesFailed;
        private final long elapsedMs;

        public Result(
            long tilesConsidered,
            long tilesWritten,
            long tilesSkippedEmpty,
            long tilesFailed,
            long elapsedMs) {

            this.tilesConsidered = tilesConsidered;
            this.tilesWritten = tilesWritten;
            this.tilesSkippedEmpty = tilesSkippedEmpty;
            this.tilesFailed = tilesFailed;
            this.elapsedMs = elapsedMs;
        }

        public long tilesConsidered() {
            return tilesConsidered;
        }

        public long tilesWritten() {
            return tilesWritten;
        }

        public long tilesSkippedEmpty() {
            return tilesSkippedEmpty;
        }

        public long tilesFailed() {
            return tilesFailed;
        }

        public long elapsedMs() {
            return elapsedMs;
        }

        @Override
        public String toString() {
            return String.format(
                "considered=%d written=%d skippedEmpty=%d failed=%d elapsed=%.1fs",
                tilesConsidered,
                tilesWritten,
                tilesSkippedEmpty,
                tilesFailed,
                elapsedMs / 1000.0);
        }
    }

    public static final class TilingException extends Exception {

        public TilingException(
            String message,
            Throwable cause) {

            super(message, cause);
        }
    }
}