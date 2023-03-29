package com.revealprecision.revealserver.api.v1.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.revealprecision.revealserver.persistence.domain.Geometry;
import com.revealprecision.revealserver.persistence.domain.LocationProperty;
import java.util.UUID;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationRequest {

  private UUID identifier;

  private String type;

  @NotNull
  private Geometry geometry;

  @NotNull
  private LocationProperty properties;
}
