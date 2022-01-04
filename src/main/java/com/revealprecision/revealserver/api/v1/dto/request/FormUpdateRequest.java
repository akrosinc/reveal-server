package com.revealprecision.revealserver.api.v1.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import javax.validation.constraints.NotBlank;
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
public class FormUpdateRequest {

  @NotBlank(message = "must not be blank")
  private String title;

  @NotNull(message = "must not be blank")
  private JsonNode payload;
}
