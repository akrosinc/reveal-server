package com.revealprecision.revealserver.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.n52.jackson.datatype.jts.JtsModule;

/*
Converts between the GeoJSON Geometry used by the API (persistence.domain.Geometry) and the JTS
Geometry stored in PostGIS columns. Geometries are always WGS84 (SRID 4326).
*/
public final class JtsGeometryUtil {

  public static final int SRID_WGS84 = 4326;

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .registerModule(new JtsModule(new GeometryFactory(new PrecisionModel(), SRID_WGS84)))
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private JtsGeometryUtil() {
  }

  // geometries deserialized by the application ObjectMapper carry no SRID; the column requires 4326
  public static Geometry withWgs84Srid(Geometry geometry) {
    if (geometry != null) {
      geometry.setSRID(SRID_WGS84);
    }
    return geometry;
  }

  public static com.revealprecision.revealserver.persistence.domain.Geometry toGeoJson(
      Geometry geometry) {
    if (geometry == null) {
      return null;
    }
    return MAPPER.convertValue(geometry,
        com.revealprecision.revealserver.persistence.domain.Geometry.class);
  }
}
