package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
//@JsonDeserialize(using = ItemDeserializer.class)
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

  private String type;

  List<Object> coordinates;

  @Override
  public String toString() {
    return "{\"type\":\"" + type +
        "\", \"coordinates\":" + coordinates +
        '}';
  }
}
