package com.revealprecision.revealserver.persistence.projection;

import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LocationMainData {

  private UUID identifier;
  private String name;

  public LocationMainData(UUID identifier, String name) {
    this.identifier = identifier;
    this.name = name;
  }
}
