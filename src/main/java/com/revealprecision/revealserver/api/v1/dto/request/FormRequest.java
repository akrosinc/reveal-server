package com.revealprecision.revealserver.api.v1.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
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
public class FormRequest {

  @Pattern(regexp = "^[0-9a-z](\\d|[0-9a-z]|-){2,30}$", message = "must match regex")
  private String name;

  @NotBlank(message = "must not be blank")
  private String title;

  @NotNull
  private boolean template;

  @NotNull
  private JsonNode payload;
}
