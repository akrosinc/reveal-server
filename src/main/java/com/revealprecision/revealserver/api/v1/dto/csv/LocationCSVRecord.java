package com.revealprecision.revealserver.api.v1.dto.csv;

import java.util.AbstractMap.SimpleEntry;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class LocationCSVRecord {

  private String identifier;
  private String name;
  private String geographicLevel;
  private Set<SimpleEntry<String, Object>> meta;


}

