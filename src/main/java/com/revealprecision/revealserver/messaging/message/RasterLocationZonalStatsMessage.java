package com.revealprecision.revealserver.messaging.message;

import java.util.List;
import java.util.Map;
import java.util.UUID;
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
public class RasterLocationZonalStatsMessage extends Message {
  private String rasterId;
  private Boolean reprocess;
  private List<String> geographicLevels;
  private UUID keycloakId;
  public boolean isReprocess() {
    return Boolean.TRUE.equals(reprocess);
  }
}
