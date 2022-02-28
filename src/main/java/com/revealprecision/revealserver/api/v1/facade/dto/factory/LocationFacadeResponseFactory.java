package com.revealprecision.revealserver.api.v1.facade.dto.factory;

import com.revealprecision.revealserver.api.v1.facade.dto.response.LocationFacade;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationFacadeResponseFactory {


  public static LocationFacade fromEntity(Location location) {
    return LocationFacade.builder().locationId(location.getIdentifier().toString())
        .name(location.getName()).build();
  }

  public static LocationFacade fromLocationEntityAndLocationRelationship(Location location,
      List<LocationRelationship> locationRelationships) {
    Optional<Location> parentLocationOptional = locationRelationships.stream()
        .filter(lr -> lr.getLocation().equals(location)).map(lr -> lr.getParentLocation())
        .findFirst();
    Location parentLocation = null;
    if (parentLocationOptional.isPresent()) {
      parentLocation = parentLocationOptional.get();
    }
    LocationFacade parentLocationFacade = fromEntity(parentLocation);
    LocationFacade locationFacade = fromEntity(location);
    locationFacade.setParentLocation(parentLocationFacade);
    return locationFacade;
  }
}
