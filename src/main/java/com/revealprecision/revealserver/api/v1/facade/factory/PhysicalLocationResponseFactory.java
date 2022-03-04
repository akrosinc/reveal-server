package com.revealprecision.revealserver.api.v1.facade.factory;

import com.revealprecision.revealserver.api.v1.facade.models.LocationPropertyFacade;
import com.revealprecision.revealserver.api.v1.facade.models.PhysicalLocation;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PhysicalLocationResponseFactory {

  public static PhysicalLocation fromEntity(Location location, Location parentLocation) {
    LocationPropertyFacade locationPropertyFacade = LocationPropertyFacade.builder()
        .name(location.getName()).geographicLevel(location.getGeographicLevel().getName())
        .parentId(parentLocation != null ? parentLocation.getIdentifier().toString() : null)
        .build();
    PhysicalLocation physicalLocation = PhysicalLocation.builder()
        .id(location.getIdentifier().toString())
        .geometry(location.getGeometry()).properties(locationPropertyFacade).build();
    return physicalLocation;
  }

  public static PhysicalLocation fromEntityLocationAndRelationship(Location location,
      List<LocationRelationship> locationRelationships) {
    Optional<Location> parentLocationOptional = locationRelationships.stream()
        .filter(lr -> lr.getLocation().equals(location) && lr.getParentLocation() != null)
        .map(LocationRelationship::getLocation).findFirst();
    Location parentLocation = null;
    if (parentLocationOptional.isPresent()) {
      parentLocation = parentLocationOptional.get();
    }
    PhysicalLocation physicalLocation = fromEntity(location, parentLocation);
    return physicalLocation;
  }

  public static List<PhysicalLocation> fromLocationsAndHierarchy(List<Location> locations,
      LocationHierarchy locationHierarchy) {
    return locations.stream()
        .map(location -> fromEntityLocationAndRelationship(location,
            locationHierarchy.getLocationRelationships()))
        .collect(
            Collectors.toList());
  }
}
