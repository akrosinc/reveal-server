package com.revealprecision.revealserver.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TileRange {

    private final int minX;
    private final int maxX;
    private final int minY;
    private final int maxY;

    public long tileCount() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1);
    }
}