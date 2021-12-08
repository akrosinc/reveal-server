package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.persistence.domain.Geometry;
import com.revealprecision.revealserver.persistence.domain.LocationProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LocationResponse {

  private UUID identifier;
  private String type;
  private Geometry geometry;
  private LocationProperty properties;
}
