package com.revealprecision.revealserver.messaging.message;

import java.util.Map;
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
public class RasterIngestionMessage extends Message {

  private String rasterId;
  private Integer minZoom;
  private Integer maxZoom;
  private Map<String, Object> metadata;
  private String layerName;
  private String valueAttribute;
}
