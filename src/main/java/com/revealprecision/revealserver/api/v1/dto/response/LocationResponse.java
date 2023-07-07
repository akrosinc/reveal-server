package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.revealprecision.revealserver.persistence.domain.Geometry;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@ToString
public class LocationResponse {

  private UUID identifier;
  private String type;
  private Geometry geometry;
  private Boolean isActive;
  private LocationPropertyResponse properties;
  List<String> ancestry;
  Map<String,Object> aggregates;
}
