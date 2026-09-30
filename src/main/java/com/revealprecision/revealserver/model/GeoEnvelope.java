package com.revealprecision.revealserver.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeoEnvelope {
  private double minX;
  private double minY;
  private double maxX;
  private double maxY;

  public double width() {
    return maxX - minX;
  }

  public double height() {
    return maxY - minY;
  }

  public boolean intersects(GeoEnvelope other) {
    return !(other.minX > maxX
        || other.maxX < minX
        || other.minY > maxY
        || other.maxY < minY);
  }
}
