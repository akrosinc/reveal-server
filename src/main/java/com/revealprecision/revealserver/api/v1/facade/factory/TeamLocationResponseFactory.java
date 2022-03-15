package com.revealprecision.revealserver.api.v1.facade.factory;

import com.revealprecision.revealserver.api.v1.facade.models.TeamLocation;
import com.revealprecision.revealserver.persistence.domain.Location;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamLocationResponseFactory {

  public static TeamLocation fromEntity(Location location) {
    return TeamLocation.builder().name(location.getName()).display(location.getName())
        .uuid(location.getIdentifier().toString()).build();
  }
}
