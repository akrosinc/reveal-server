package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class Geometry implements Serializable {

  public enum GeometryType {
    @JsonProperty("Point")
    POINT("Point"),
    @JsonProperty("Polygon")
    POLYGON("Polygon"),
    @JsonProperty("MultiPolygon")
    MULTI_POLYGON("MultiPolygon");
    private final String value;

    GeometryType(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }

  private GeometryType type;
  List<Object> coordinates;
}
