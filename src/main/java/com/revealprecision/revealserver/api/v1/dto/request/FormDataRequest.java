package com.revealprecision.revealserver.api.v1.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import javax.validation.constraints.NotNull;
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
public class FormDataRequest {

  private UUID formIdentifier;

  @NotNull
  private JsonNode payload;
}
