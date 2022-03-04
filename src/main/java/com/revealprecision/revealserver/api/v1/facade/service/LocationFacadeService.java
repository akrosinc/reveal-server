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

  public List<Location> syncLocations(LocationSyncRequest locationSyncRequest,
      LocationHierarchy hierarchy) {
    boolean isJurisdiction = locationSyncRequest.getIsJurisdiction();
    List<Location> locations;
    if (isJurisdiction) {
      locations = getLocationsByJurisdictions(locationSyncRequest);
    } else {
      locations = geStructures(locationSyncRequest, hierarchy);
    }
    return locations;
  }

  public HttpHeaders addCountToHeaders(Long count,
      HttpHeaders headers) {
    headers.add(TOTAL_RECORDS, String.valueOf(count));
    return headers;
  }

  public Set<String> saveSyncedLocations(List<PhysicalLocation> physicalLocationRequests) {
    Set<String> locationRequestsWithErrors = new HashSet<>();
    List<LocationRequest> locationRequests = LocationRequestFactory
        .fromPhysicalLocationRequests(physicalLocationRequests);
    for (LocationRequest locationRequest : locationRequests) {
      try {
        locationService.createLocation(locationRequest);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        locationRequestsWithErrors.add(locationRequest.getProperties().getExternalId().toString());
      }
    }
    return locationRequestsWithErrors;
  }


  private List<Location> geStructures(LocationSyncRequest locationSyncRequest,
      LocationHierarchy hierarchy) {
    List<Location> locations = new ArrayList<>();
    List<String> requestParentIds = locationSyncRequest.getParentId();
    if (requestParentIds != null && !requestParentIds.isEmpty()) {
      List<UUID> parentIdentifiers = extractLocationIdentifiers(
          locationSyncRequest.getParentId());
      locations = locationService
          .getLocationsByParentIdentifiers(parentIdentifiers, hierarchy);
    }
    return locations;
  }

  private List<Location> getLocationsByJurisdictions(LocationSyncRequest locationSyncRequest) {
    List<Location> locations;
    List<String> requestLocationIds = locationSyncRequest.getLocationIds();
    if (requestLocationIds != null && !requestLocationIds.isEmpty()) {
      List<UUID> locationIdentifiers = extractLocationIdentifiers(
          locationSyncRequest.getLocationIds());
      locations = locationService.getAllByIdentifiers(locationIdentifiers);
    } else {
      locations = locationService.getAllByNames(locationSyncRequest.getLocationNames());
    }
    return locations;
  }

  private List<UUID> extractLocationIdentifiers(List<String> locationIds) {
    return locationIds.stream().map(UUID::fromString)
        .collect(
            Collectors.toList());
  }

}
