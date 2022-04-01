package com.revealprecision.revealserver.batch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.revealprecision.revealserver.persistence.domain.Geometry;
import com.revealprecision.revealserver.persistence.domain.LocationProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationBatchDTO {

  private String type;
  private LocationProperty properties;
  @JsonProperty("geometry")
  private Geometry geometry;
}
