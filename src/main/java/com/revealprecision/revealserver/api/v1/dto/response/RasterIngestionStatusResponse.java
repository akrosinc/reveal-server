package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.enums.IngestionStage;
import java.time.LocalDateTime;
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
public class RasterIngestionStatusResponse {

  private IngestionStage stage;
  private String message;
  private LocalDateTime lastUpdated;
}
