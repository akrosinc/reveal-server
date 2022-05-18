package com.revealprecision.revealserver.api.v1.facade.factory;

import com.revealprecision.revealserver.api.v1.facade.models.LocationFacade;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationFacadeResponseFactory {


  public static LocationFacade fromEntity(Location location) {
    Set<String> locationTags = new HashSet<>();
    locationTags.add(location.getGeographicLevel().getName());
    LocationFacade locationFacade = LocationFacade.builder()
        .locationId(location.getIdentifier().toString())
        .name(location.getName()).tags(locationTags).build();
    locationFacade.setServerVersion(location.getServerVersion());
    return locationFacade;
  }

  public static LocationFacade fromLocationEntityAndLocationRelationship(Location location,
      List<LocationRelationship> locationRelationships) {
    Location parentLocation = null;
    Optional<Location> parentLocationOptional = locationRelationships.stream()
        .filter(lr -> lr.getLocation().equals(location) && lr.getParentLocation() != null)
        .map(LocationRelationship::getParentLocation).findFirst();
    if (parentLocationOptional.isPresent()) {
      parentLocation = parentLocationOptional.get();
    }
    LocationFacade parentLocationFacade =
        parentLocation != null ? fromEntity(parentLocation) : null;
    LocationFacade locationFacade = fromEntity(location);
    locationFacade.setParentLocation(parentLocationFacade);
    return locationFacade;
  }
}
