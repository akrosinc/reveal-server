package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class Geometry implements Serializable {

  public enum GeometryType {
    @JsonProperty("Point")
    POINT,
    @JsonProperty("Polygon")
    POLYGON,
    @JsonProperty("MultiPolygon")
    MULTI_POLYGON
  }

  private GeometryType type;
  List<Object> coordinates;
}
