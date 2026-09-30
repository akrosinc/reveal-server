package com.revealprecision.revealserver.raster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Converts a flat pixel-value buffer into a compact list of {@link PixelRect}
 * rectangles, so that large uniform regions become a handful of shapes
 * instead of one polygon per pixel.
 *
 * Algorithm: horizontal run-length-encode each row, then merge vertically
 * adjacent rows whose run pattern is identical (same segment boundaries and
 * values) into a single taller rectangle.
 */
public final class PixelRectExtractor {

    private PixelRectExtractor() {
    }

    /**
     * @param buffer      tileSize*tileSize row-major pixel values
     * @param tileSize    source buffer dimension (e.g. 256)
     * @param tileExtent  output MVT tile-pixel extent (e.g. 4096)
     * @param nodataValue pixel value to skip entirely (may be null to keep everything)
     */
    public static List<PixelRect> extract(int[] buffer, int tileSize, int tileExtent, Integer nodataValue) {
        if (buffer == null || buffer.length == 0) {
            return Collections.emptyList();
        }
        if (buffer.length != tileSize * tileSize) {
            throw new IllegalArgumentException("buffer size " + buffer.length + " != tileSize^2 " + (tileSize * tileSize));
        }

        // Fast path: check if tile is completely uniform (all pixels same value)
        int firstVal = buffer[0];
        boolean uniform = true;
        for (int i = 1; i < buffer.length; i++) {
            if (buffer[i] != firstVal) {
                uniform = false;
                break;
            }
        }
        if (uniform) {
            if (nodataValue != null && firstVal == nodataValue) {
                return Collections.emptyList();
            }
            return Collections.singletonList(new PixelRect(0, 0, tileExtent, tileExtent, firstVal));
        }

        double scale = (double) tileExtent / tileSize;

        // Step 1: horizontal RLE per row packed into int arrays [startCol, endCol, value, ...]
        int[][] rowRuns = new int[tileSize][];
        int[] tempRowBuffer = new int[tileSize * 3];

        for (int row = 0; row < tileSize; row++) {
            rowRuns[row] = encodeRowRuns(buffer, row, tileSize, tempRowBuffer);
        }

        // Step 2: merge vertically adjacent rows with identical run patterns
        List<PixelRect> result = new ArrayList<>();
        int row = 0;
        while (row < tileSize) {
            int[] currentRuns = rowRuns[row];
            int spanEnd = row + 1;
            while (spanEnd < tileSize && Arrays.equals(rowRuns[spanEnd], currentRuns)) {
                spanEnd++;
            }

            int runCount = currentRuns.length / 3;
            for (int i = 0; i < runCount; i++) {
                int startCol = currentRuns[i * 3];
                int endCol = currentRuns[i * 3 + 1];
                int value = currentRuns[i * 3 + 2];

                if (nodataValue != null && value == nodataValue) {
                    continue;
                }
                result.add(new PixelRect(
                        startCol * scale, row * scale,
                        endCol * scale, spanEnd * scale,
                        value));
            }
            row = spanEnd;
        }

        return result;
    }

    private static int[] encodeRowRuns(int[] buffer, int row, int tileSize, int[] temp) {
        int rowOffset = row * tileSize;
        int runStart = 0;
        int runVal = buffer[rowOffset];
        int runIdx = 0;

        for (int col = 1; col <= tileSize; col++) {
            int val = (col < tileSize) ? buffer[rowOffset + col] : Integer.MIN_VALUE;
            if (val != runVal) {
                temp[runIdx++] = runStart;
                temp[runIdx++] = col;
                temp[runIdx++] = runVal;
                runStart = col;
                runVal = val;
            }
        }

        return Arrays.copyOf(temp, runIdx);
    }
}
