package com.revealprecision.revealserver.api.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
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
public class GeographicLevelRequest {

  @NotBlank(message = "must not be empty")
  private String title;

  @Pattern(regexp = "[a-z0-9\\-]+", message = "pattern not matched")
  @NotBlank(message = "must not be empty")
  private String name;
}
