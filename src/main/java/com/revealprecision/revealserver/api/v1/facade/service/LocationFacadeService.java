package com.revealprecision.revealserver.api.v1.facade.service;

import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.api.v1.facade.factory.LocationRequestFactory;
import com.revealprecision.revealserver.api.v1.facade.models.PhysicalLocation;
import com.revealprecision.revealserver.api.v1.facade.request.LocationSyncRequest;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.service.LocationService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationFacadeService {

  public static final String TOTAL_RECORDS = "total_records";

  private final LocationService locationService;

  public List<Location> syncLocations(LocationSyncRequest locationSyncRequest, LocationHierarchy hierarchy) {
    boolean isJurisdiction = locationSyncRequest.getIsJurisdiction();
    List<Location> locations = null;
    if (isJurisdiction) {

      if (locationSyncRequest.getLocationIds() != null && !locationSyncRequest.getLocationIds()
          .isEmpty()) {
        List<UUID> locationIds = locationSyncRequest.getLocationIds().stream().map(UUID::fromString)
            .collect(
                Collectors.toList());
        locations = locationService.getAllByIdentifiers(locationIds);
      } else {
        locations = locationService.getAllByNames(locationSyncRequest.getLocationNames());
      }
    } else {

      if (locationSyncRequest.getParentId() != null && !locationSyncRequest.getParentId()
          .isEmpty()) {
        List<UUID> parentIdentifiers = locationSyncRequest.getParentId().stream()
            .map(UUID::fromString).collect(
                Collectors.toList());
        locations = locationService
            .getLocationsByParentIdentifiers(parentIdentifiers, hierarchy);
      }
    }
    return locations;
  }

  public HttpHeaders addCountToHeaders(Long count,
      HttpHeaders headers) {
    headers.add(TOTAL_RECORDS, String.valueOf(count));
    return headers;
  }

  public Set<String> saveSyncedLocations(List<PhysicalLocation> physicalLocationRequests){
    Set<String> locationRequestsWithErrors = new HashSet<>();
    List<LocationRequest> locationRequests = LocationRequestFactory.fromPhysicalLocationRequests(physicalLocationRequests);
    for(LocationRequest locationRequest:locationRequests){
        try {
          locationService.createLocation(locationRequest);
        }catch (Exception e){
          log.error(e.getMessage(), e);
          locationRequestsWithErrors.add(locationRequest.getProperties().getExternalId().toString());
        }
    }
    return  locationRequestsWithErrors;
  }

}
