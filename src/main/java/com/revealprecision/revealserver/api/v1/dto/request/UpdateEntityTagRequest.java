package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEntityTagRequest {

  private UUID identifier;
  private boolean simulationDisplay;

}