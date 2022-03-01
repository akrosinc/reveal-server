package com.revealprecision.revealserver.api.v1.facade.service;

import com.revealprecision.revealserver.api.v1.facade.request.LocationSyncRequest;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.service.LocationService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationFacadeService {

  public static final String TOTAL_RECORDS = "total_records";

  private final LocationService locationService;

  public List<Location> syncLocations(LocationSyncRequest locationSyncRequest) {
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
            .getLocationsByParentIdentifiers(parentIdentifiers, null);//TODO: we need the hierarchy
      }
    }
    return locations;
  }

  public HttpHeaders addCountToHeaders(Long count,
      HttpHeaders headers) {
    headers.add(TOTAL_RECORDS, String.valueOf(count));
    return headers;
  }

}
