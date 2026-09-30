package com.revealprecision.revealserver.raster;

import lombok.Getter;

@Getter
public class PixelRect {

    private final double minX;
    private final double minY;
    private final double maxX;
    private final double maxY;
    private final int value;

    public PixelRect(double minX, double minY, double maxX, double maxY, int value) {
        if (maxX <= minX || maxY <= minY) {
            throw new IllegalArgumentException(
                    "Degenerate rectangle: (" + minX + "," + minY + ") -> (" + maxX + "," + maxY + ")"
            );
        }

        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.value = value;
    }

    public double width() {
        return maxX - minX;
    }

    public double height() {
        return maxY - minY;
    }
}