package com.revealprecision.revealserver.api.v1.facade.service;

import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.api.v1.facade.factory.LocationRequestFactory;
import com.revealprecision.revealserver.api.v1.facade.factory.PhysicalLocationResponseFactory;
import com.revealprecision.revealserver.api.v1.facade.models.CreateLocationRequest;
import com.revealprecision.revealserver.api.v1.facade.models.PhysicalLocation;
import com.revealprecision.revealserver.api.v1.facade.request.LocationSyncRequest;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.projection.LocationWithParentProjection;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.LocationService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

  private final LocationRelationshipService locationRelationshipService;

  public List<PhysicalLocation> syncLocations(LocationSyncRequest locationSyncRequest,
      UUID hierarchyIdentifier) {
    boolean isJurisdiction = locationSyncRequest.getIsJurisdiction();
    List<PhysicalLocation> physicalLocations;
    if (isJurisdiction) {
      physicalLocations = getLocationsByJurisdictionsWithoutGeometry(locationSyncRequest);
    } else {
      physicalLocations = getStructuresWithoutGeometry(locationSyncRequest, hierarchyIdentifier);
    }

    List<UUID> collect = physicalLocations.stream().map(PhysicalLocation::getId)
        .map(UUID::fromString)
        .collect(Collectors.toList());

    Map<UUID, Location> locationsByIdentifierList = locationService.getLocationsByIdentifierList(
        collect);

    return physicalLocations.stream()
        .peek(physicalLocation -> physicalLocation.setGeometry(
            locationsByIdentifierList.get(UUID.fromString(physicalLocation.getId()))
                .getGeometry())).collect(Collectors.toList());

  }

  public HttpHeaders addCountToHeaders(Long count,
      HttpHeaders headers) {
    headers.add(TOTAL_RECORDS, String.valueOf(count));
    return headers;
  }

  public Set<String> saveSyncedLocations(List<CreateLocationRequest> createLocationRequests) {
    Set<String> locationRequestsWithErrors = new HashSet<>();
    Map<LocationRequest, UUID> locationRequestWithParent = LocationRequestFactory
        .fromPhysicalLocationRequests(createLocationRequests);
    for (Entry<LocationRequest, UUID> locationRequestUUIDEntry : locationRequestWithParent.entrySet()) {
      try {
        locationService.createLocation(locationRequestUUIDEntry.getKey(),
            locationRequestUUIDEntry.getValue());
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        locationRequestsWithErrors.add(
            locationRequestUUIDEntry.getKey().getProperties().getExternalId().toString());
      }
    }
    return locationRequestsWithErrors;
  }


  private List<PhysicalLocation> getStructuresWithoutGeometry(LocationSyncRequest locationSyncRequest,
      UUID hierarchyIdentifier) {
    if (locationSyncRequest.getParentId() != null && !locationSyncRequest.getParentId().isEmpty()) {
       return locationRelationshipService
          .getChildrenByGeoLevelNameWithinLocationListHierarchyAndServerVersion(
              extractLocationIdentifiers(locationSyncRequest.getParentId())
              , hierarchyIdentifier
              , locationSyncRequest.getServerVersion()
              , LocationConstants.STRUCTURE
          )
          .stream().map(PhysicalLocationResponseFactory::fromLocationWithParentProjection)
          .collect(Collectors.toList());
    }
    return new ArrayList<>();
  }

  private List<PhysicalLocation> getLocationsByJurisdictionsWithoutGeometry(
      LocationSyncRequest locationSyncRequest) {
    List<LocationWithParentProjection> locations;
    List<String> requestLocationIds = locationSyncRequest.getLocationIds();
    if (requestLocationIds != null && !requestLocationIds.isEmpty()) {
      List<UUID> locationIdentifiers = extractLocationIdentifiers(
          locationSyncRequest.getLocationIds());
      //TODO: until we find way to differentiate target level and structures
      locations = locationService.getAllNotStructuresByIdentifiersAndServerVersion(
          locationIdentifiers, locationSyncRequest.getServerVersion());
    } else {
      //TODO: until we find way to differentiate target level and structures
      locations = locationService.getAllNotStructureByNamesAndServerVersion(
          locationSyncRequest.getLocationNames(), locationSyncRequest.getServerVersion());
    }
    return locations.stream()
        .map(PhysicalLocationResponseFactory::fromLocationWithParentProjection)
        .collect(
            Collectors.toList());
  }

  private List<UUID> extractLocationIdentifiers(List<String> locationIds) {
    return locationIds.stream().map(UUID::fromString)
        .collect(
            Collectors.toList());
  }

}
