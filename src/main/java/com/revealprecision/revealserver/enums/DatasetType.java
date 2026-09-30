package com.revealprecision.revealserver.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DatasetType {
    RASTER("Raster"),
    JSON("JSON");

    private final String value;

    DatasetType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DatasetType fromString(String text) {
        if (text == null) {
            return null;
        }
        for (DatasetType b : DatasetType.values()) {
            if (b.name().equalsIgnoreCase(text) || b.value.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }
}
