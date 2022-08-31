package com.revealprecision.revealserver.api.v1.facade.factory;

import com.revealprecision.revealserver.api.v1.facade.models.LocationFacade;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.domain.Plan;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationFacadeResponseFactory {


  public static LocationFacade fromEntity(Location location, Set<Plan> plans) {
    Set<String> locationTags = new HashSet<>();
    locationTags.add(location.getGeographicLevel().getName());
    Set<String> assignedPlans = plans.stream().map(Plan::getIdentifier).map(UUID::toString).collect(
        Collectors.toSet());
    LocationFacade locationFacade = LocationFacade.builder()
        .locationId(location.getIdentifier().toString())
        .name(location.getName()).tags(locationTags).planIds(assignedPlans).build();
    locationFacade.setServerVersion(location.getServerVersion());
    return locationFacade;
  }

  public static LocationFacade fromLocationEntityAndLocationRelationship(Location location,
      List<LocationRelationship> locationRelationships, Set<Plan> plans) {
    Location parentLocation = null;
    Optional<Location> parentLocationOptional = locationRelationships.stream()
        .filter(lr -> lr.getLocation().equals(location) && lr.getParentLocation() != null)
        .map(LocationRelationship::getParentLocation).findFirst();
    if (parentLocationOptional.isPresent()) {
      parentLocation = parentLocationOptional.get();
    }
    LocationFacade parentLocationFacade =
        parentLocation != null ? fromEntity(parentLocation, plans) : null;
    LocationFacade locationFacade = fromEntity(location, plans);
    locationFacade.setParentLocation(parentLocationFacade);
    return locationFacade;
  }
}
