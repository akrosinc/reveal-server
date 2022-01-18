package com.revealprecision.revealserver.persistence.domain;

import java.util.UUID;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationProperty {

  @NotBlank
  private String name;

  @NotNull
  private String status;

  private UUID externalId;
  @NotNull
  private String geographicLevel;
}