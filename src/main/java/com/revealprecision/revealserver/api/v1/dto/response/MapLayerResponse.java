package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.enums.LayerType;
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
public class MapLayerResponse {

  private UUID id;
  private String name;
  private String layerIdentifier;
  private LayerType type;
}
