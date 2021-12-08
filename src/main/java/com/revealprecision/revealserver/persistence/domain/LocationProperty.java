package com.revealprecision.revealserver.persistence.domain;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationProperty {
  private String name;
  private String status;
  private UUID externalId;
  private String geographicLevel;
}
