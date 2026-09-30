package com.revealprecision.revealserver.raster;

import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.MvtEncoder;
import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.UserDataKeyValueMapConverter;
import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.model.JtsLayer;
import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.model.JtsMvt;
import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerParams;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

/**
 * Builds a single-layer Mapbox Vector Tile from a set of {@link PixelRect}
 * rectangles that are already expressed in tile-pixel coordinates (0..tileExtent, Y down).
 */
public final class RasterMvtEncoder {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private final String layerName;
    private final String valueAttribute;
    private final MvtLayerParams layerParams;
    private final UserDataKeyValueMapConverter converter = new UserDataKeyValueMapConverter();

    public RasterMvtEncoder(String layerName, String valueAttribute, int tileExtent) {
        this.layerName = layerName != null ? layerName : "raster";
        this.valueAttribute = valueAttribute != null ? valueAttribute : "value";
        this.layerParams = new MvtLayerParams(tileExtent > 0 ? tileExtent : MvtLayerParams.DEFAULT_EXTENT);
    }

    public RasterMvtEncoder(String layerName, String valueAttribute) {
        this(layerName, valueAttribute, MvtLayerParams.DEFAULT_EXTENT);
    }

    /** @return protobuf-encoded MVT tile bytes, or {@code null} if there are no rectangles to encode. */
    public byte[] encode(List<PixelRect> rects) {
        if (rects == null || rects.isEmpty()) {
            return null;
        }

        List<Geometry> geoms = new ArrayList<>(rects.size());
        for (PixelRect r : rects) {
            Polygon poly = toPolygon(r);
            Map<String, Object> userData = Collections.singletonMap(valueAttribute, r.getValue());
            poly.setUserData(userData);
            geoms.add(poly);
        }

        JtsLayer layer = new JtsLayer(layerName, geoms, layerParams.getExtent());
        JtsMvt mvt = new JtsMvt(layer);

        return MvtEncoder.encode(mvt, layerParams, converter);
    }

    private Polygon toPolygon(PixelRect r) {
        Coordinate[] shell = new Coordinate[]{
                new Coordinate(r.getMinX(), r.getMinY()),
                new Coordinate(r.getMaxX(), r.getMinY()),
                new Coordinate(r.getMaxX(), r.getMaxY()),
                new Coordinate(r.getMinX(), r.getMaxY()),
                new Coordinate(r.getMinX(), r.getMinY())
        };
        return GEOMETRY_FACTORY.createPolygon(shell);
    }
}
