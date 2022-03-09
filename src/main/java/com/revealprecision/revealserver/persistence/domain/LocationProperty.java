package com.revealprecision.revealserver.persistence.domain;

import java.util.UUID;
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
public class LocationProperty {

  @NotBlank
  private String name;

  @NotNull
  private String status;

  private UUID externalId;
  @NotNull
  private String geographicLevel;
}
