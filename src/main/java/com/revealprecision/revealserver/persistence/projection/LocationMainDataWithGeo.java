package com.revealprecision.revealserver.persistence.projection;

import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LocationMainDataWithGeo {

  private UUID identifier;
  private String name;
  private String geographicLevelName;

  public LocationMainDataWithGeo(UUID identifier, String name,String geographicLevelName) {
    this.identifier = identifier;
    this.name = name;
    this.geographicLevelName = geographicLevelName;
  }
}
