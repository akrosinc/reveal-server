package com.revealprecision.revealserver.api.v1.facade.service;

import com.revealprecision.revealserver.api.v1.facade.request.LocationSyncRequest;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.service.LocationHierarchyService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationHierarchyFacadeService {

  private final LocationHierarchyService locationHierarchyService;


  public LocationHierarchy getRequestedOrReturnDefault(LocationSyncRequest locationSyncRequest) {
    String hierarchyIdentifier = locationSyncRequest.getHierarchyIdentifier();
    if(hierarchyIdentifier != null){
      return locationHierarchyService.findByIdentifier(UUID.fromString(hierarchyIdentifier));
    }
    return locationHierarchyService.findByName("default").get(0);
  }

  public UUID getIdRequestedOrReturnDefault(LocationSyncRequest locationSyncRequest) {
    String hierarchyIdentifier = locationSyncRequest.getHierarchyIdentifier();
    if(hierarchyIdentifier != null){
      return locationHierarchyService.findNativeById(UUID.fromString(hierarchyIdentifier));
    }
    return locationHierarchyService.findNativeByName("default");
  }
}
