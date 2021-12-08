package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.persistence.domain.Geometry;
import com.revealprecision.revealserver.persistence.domain.LocationProperty;
import javax.validation.constraints.NotNull;
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
public class LocationRequest {

  private String type;

  @NotNull
  private Geometry geometry;

  private LocationProperty properties;
}
