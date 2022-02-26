package com.revealprecision.revealserver.api.v1.facade.dto.factory;

import com.revealprecision.revealserver.api.v1.facade.dto.response.LocationFacade;
import com.revealprecision.revealserver.persistence.domain.Location;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationFacadeResponseFactory {


  public static LocationFacade fromLocationEntityWithoutParent(Location location){
    LocationFacade locationFacade = LocationFacade.builder().locationId(location.getIdentifier().toString()).name(location.getName()).build();
    return locationFacade;
  }
}
