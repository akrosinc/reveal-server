package com.revealprecision.revealserver.api.v1.dto.response;

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
public class GeographicLevelResponse {

  private UUID identifier;
  private String title;
  private String name;
}
