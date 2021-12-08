package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.LocationHierarchyRequest;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.repository.LocationHierarchyRepository;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class LocationHierarchyService {

  private LocationHierarchyRepository locationHierarchyRepository;
  private LocationRelationshipService locationRelationshipService;
  private JobScheduler jobScheduler;
  private GeographicLevelService geographicLevelService;

  @Autowired
  public LocationHierarchyService(LocationHierarchyRepository locationHierarchyRepository,
      LocationRelationshipService locationRelationshipService,
      JobScheduler jobScheduler,
      GeographicLevelService geographicLevelService) {
    this.locationHierarchyRepository = locationHierarchyRepository;
    this.locationRelationshipService = locationRelationshipService;
    this.jobScheduler = jobScheduler;
    this.geographicLevelService = geographicLevelService;
  }

  public LocationHierarchy createLocationHierarchy(
      LocationHierarchyRequest locationHierarchyRequest) {
    LocationHierarchy locationHierarchy = locationHierarchyRepository.save(
        LocationHierarchy.builder().nodeOrder(locationHierarchyRequest.getNodeOrder()).build());
    if (locationHierarchy != null) {
      jobScheduler.enqueue(
          () -> locationRelationshipService.createLocationRelationships(locationHierarchy));
    }
    return locationHierarchy;
  }

  public Page<LocationHierarchy> getLocationHierarchies(Integer pageNumber, Integer pageSize) {
    return locationHierarchyRepository.findAll(PageRequest.of(pageNumber, pageSize));
  }

  public Boolean isLocationHierarchyExists(LocationHierarchy locationHierarchy) {
    //TODO: implement
    return false;
  }
}
