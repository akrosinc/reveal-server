package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
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
public class FormResponse {

  private UUID identifier;
  private String name;
  private String title;
  private boolean template;
  private JsonNode payload;
}
