package com.revealprecision.revealserver.api.v1.dto.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.response.EntityMetadataResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationPropertyResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.persistence.domain.Geometry;
import com.revealprecision.revealserver.persistence.projection.LocationWithMetadataProjection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationResponsesFromProjectionsFactory {

  public static List<LocationResponse> buildLocationResponsesFromProjections(
      List<LocationWithMetadataProjection> projections,Map<String, UUID> datasetTagMap,
      ObjectMapper objectMapper) {



    // Group projections by locationId
    Map<String, List<LocationWithMetadataProjection>> groupedByLocation = projections.stream()
        .collect(Collectors.groupingBy(LocationWithMetadataProjection::getId));

    return groupedByLocation.entrySet().stream().map(entry -> {
      String locationId = entry.getKey();
      List<LocationWithMetadataProjection> locationRows = entry.getValue();
      LocationWithMetadataProjection firstRow = locationRows.get(0);

      LocationResponse locationResponse = new LocationResponse();
      locationResponse.setIdentifier(UUID.fromString(locationId));
      locationResponse.setType(firstRow.getType() != null ? firstRow.getType() : "Feature");

      // Parse geometry
      try {
        if (firstRow.getGeometry() != null) {
          locationResponse.setGeometry(
              objectMapper.readValue(firstRow.getGeometry(), Geometry.class));
        }
      } catch (JsonProcessingException e) {
        log.warn("Could not parse geometry for location: {}", locationId);
      }

      // rowByTag: tagName -> projection row
      Map<String, LocationWithMetadataProjection> rowByTag = locationRows.stream()
          .filter(row -> row.getTag() != null)
          .collect(Collectors.toMap(
              LocationWithMetadataProjection::getTag,
              row -> row,
              (a, b) -> a
          ));

      // Build metadata from datasetTagMap - all datasets represented
      List<EntityMetadataResponse> metadata = datasetTagMap.entrySet().stream()
          .map(e -> {
            String tagName = e.getKey();
            UUID datasetId = e.getValue();
            LocationWithMetadataProjection row = rowByTag.get(
                getRefenceTagName(tagName));
            Double value = row != null
                ? getValueByAggregationType(tagName, row)
                : null;
            return new EntityMetadataResponse(value, tagName, "IMPORT", datasetId);
          })
          .collect(Collectors.toList());

      LocationPropertyResponse properties = new LocationPropertyResponse();
      properties.setName(firstRow.getName());
      locationResponse.setType("Feature");
      properties.setGeographicLevel(firstRow.getGeographicLevel());
      properties.setMetadata(metadata);
      locationResponse.setProperties(properties);

      return locationResponse;
    }).collect(Collectors.toList());
  }


  public static  Map<String,List<EntityMetadataResponse>> buildEntityMetadataResponseMapFromProjectionsWithoutGeom(
      List<LocationWithMetadataProjection> projections,Map<String, UUID> datasetTagMap) {

    Map<String, List<LocationWithMetadataProjection>> groupedByLocation = projections.stream()
        .collect(Collectors.groupingBy(LocationWithMetadataProjection::getId));

    return groupedByLocation.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> {
              List<LocationWithMetadataProjection> locationRows = entry.getValue();

              // rowByTag: fieldcode -> projection row
              Map<String, LocationWithMetadataProjection> rowByTag = locationRows.stream()
                  .filter(row -> row.getTag() != null)
                  .collect(Collectors.toMap(
                      LocationWithMetadataProjection::getTag,
                      row -> row,
                      (a, b) -> a
                  ));

              // Build metadata from datasetTagMap - all datasets represented
              return datasetTagMap.entrySet().stream()
                  .map(e -> {
                    String tagName = e.getKey();
                    UUID datasetId = e.getValue();
                    LocationWithMetadataProjection row = rowByTag.get(
                        getRefenceTagName(tagName));
                    Double value = row != null
                        ? getValueByAggregationType(tagName, row)
                        : null;
                    return new EntityMetadataResponse(
                        value, tagName, "IMPORT", datasetId);
                  })
                  .collect(Collectors.toList());
            }
        ));
  }


  public static  String getRefenceTagName(String aggregateTagName) {
    String[] parts = aggregateTagName.split("-(?=[^-]+$)");
    return parts[0];
  }


  public static  Double getValueByAggregationType(String tagName, LocationWithMetadataProjection row) {
    String[] parts = tagName.split("-(?=[^-]+$)");
    if (parts.length != 2) return row.getSum();

    switch (parts[1].toLowerCase()) {
      case "sum":    return row.getSum();
      case "avg":    return row.getAvg();
      case "median": return row.getMedian();
      case "min":    return row.getMin();
      case "max":    return row.getMax();
      default:       return row.getSum();
    }
  }
}
