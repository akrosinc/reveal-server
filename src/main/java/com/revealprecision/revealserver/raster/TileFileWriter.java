package com.revealprecision.revealserver.raster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

/**
 * Writes MVT tiles as individual files under {@code outputDir/{z}/{x}/{y}.mvt}
 * (the standard XYZ tile layout consumable directly by tile servers and map clients).
 *
 * Thread-safe: cached directory creation avoids redundant OS calls.
 */
public final class TileFileWriter implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(TileFileWriter.class);

    private final Path outputDir;
    private final boolean gzip;
    private final AtomicLong totalWritten = new AtomicLong();
    private final Set<Path> createdDirs = ConcurrentHashMap.newKeySet();

    public TileFileWriter(Path outputDir, boolean gzip) throws IOException {
        this.outputDir = outputDir.toAbsolutePath().normalize();
        this.gzip = gzip;
        Files.createDirectories(this.outputDir);
        createdDirs.add(this.outputDir);
    }

    /** Writes one tile to {@code outputDir/{z}/{x}/{y}.mvt} (or {@code .mvt.gz} if gzip is enabled). */
    public void writeTile(int z, int x, int y, byte[] mvtBytes) {
        if (mvtBytes == null || mvtBytes.length == 0) {
            return;
        }

        try {
            Path dir = outputDir.resolve(String.valueOf(z)).resolve(String.valueOf(x));
            ensureDirectoryExists(dir);

            String filename = y + (gzip ? ".mvt.gz" : ".mvt");
            Path tileFile = dir.resolve(filename);

            byte[] data = gzip ? gzip(mvtBytes) : mvtBytes;
            Files.write(tileFile, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            long written = totalWritten.incrementAndGet();
            if (written % 5000 == 0) {
                log.info("Written {} tile files so far", written);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write tile " + z + "/" + x + "/" + y, e);
        }
    }

    private void ensureDirectoryExists(Path dir) throws IOException {
        if (!createdDirs.contains(dir)) {
            Files.createDirectories(dir);
            createdDirs.add(dir);
        }
    }

    /** Writes a minimal TileJSON-style metadata.json at the root of the output directory. */
    public void writeMetadata(String name, String description, int minZoom, int maxZoom,
                              double[] boundsLonLat, String layerName, String valueAttribute) {
        String json = String.format(
            "{"
            + "\"tilejson\": \"2.2.0\","
            + "\"name\": \"%s\","
            + "\"description\": \"%s\","
            + "\"format\": \"pbf\","
            + "\"minzoom\": %d,"
            + "\"maxzoom\": %d,"
            + "\"bounds\": [%.6f, %.6f, %.6f, %.6f],"
            + "\"tiles\": [\"{z}/{x}/{y}.mvt%s\"],"
            + "\"vector_layers\": ["
            + "{\"id\": \"%s\","
            + "\"fields\": {\"%s\": \"Number\"},"
            + "\"minzoom\": %d,"
            + "\"maxzoom\": %d}"
            + "]"
            + "}",
            escapeJson(name),
            escapeJson(description),
            minZoom,
            maxZoom,
            boundsLonLat[0],
            boundsLonLat[1],
            boundsLonLat[2],
            boundsLonLat[3],
            gzip ? ".gz" : "",
            escapeJson(layerName),
            escapeJson(valueAttribute),
            minZoom,
            maxZoom
        );

        try {
            Files.writeString(outputDir.resolve("metadata.json"), json,
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write metadata.json", e);
        }
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public long getTotalWritten() {
        return totalWritten.get();
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.max(64, data.length / 2));
        try (GZIPOutputStream gz = new GZIPOutputStream(baos)) {
            gz.write(data);
        }
        return baos.toByteArray();
    }

    @Override
    public void close() {
        log.info("Tile file writer closed. Total tiles written: {}", totalWritten.get());
    }
}
