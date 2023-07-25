package com.revealprecision.revealserver.service.models;

import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import com.revealprecision.revealserver.persistence.es.LocationElastic;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationResourcePlanning {

  private UUID identifier;
  private String name;
  private String geographicLevel;
  private List<String> ancestry;
  private Map<String, ColumnData> columnDataMap = new LinkedHashMap<>();

  public LocationResourcePlanning(LocationElastic locationElastic, Object populationCount,
      Object structureCount, List<String> ancestry) {
    this.identifier = UUID.fromString(locationElastic.getId());
    this.name = locationElastic.getName();
    this.geographicLevel = locationElastic.getLevel();
    this.ancestry = ancestry;
    if (populationCount != null) {
      columnDataMap.put("Official population",
          ColumnData.builder().key("official-population")
              .isPercentage(false).dataType("integer").value(populationCount).build());
    } else {
      columnDataMap.put("Official population",
          ColumnData.builder().key("official-population")
              .isPercentage(false).dataType("integer").value(0.0).build());
    }
    if (structureCount != null) {
      columnDataMap.put("Number of structures in the campaign location",
          ColumnData.builder().key("structure-count")
              .isPercentage(false).dataType("integer").value(structureCount).build());
    } else {
      columnDataMap.put("Number of structures in the campaign location",
          ColumnData.builder().key("structure-count")
              .isPercentage(false).dataType("integer").value(0.0).build());
    }
  }
}
