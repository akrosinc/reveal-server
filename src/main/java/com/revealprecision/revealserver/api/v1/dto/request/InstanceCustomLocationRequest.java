package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.LocationSubtypeEnum;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InstanceCustomLocationRequest {

  private UUID parentLocationIdentifier;

  private LocationSubtypeEnum locationSubtype;

  private String name;

  private String boundaryGeographicLevelName;

  private String pointGeographicLevelName;

  private Geometry locationBoundary;

  private List<Point> points;

  /**
   * Numeric entity tag counted on the parent location: incremented by one on create, and moved from
   * the previous parent to the new one when an update changes the parent. Not stored.
   */
  private String tagName;
}
