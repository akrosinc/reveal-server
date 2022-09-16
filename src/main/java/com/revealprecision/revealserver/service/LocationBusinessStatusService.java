package com.revealprecision.revealserver.service;


import com.revealprecision.revealserver.persistence.domain.LocationAboveStructure;
import com.revealprecision.revealserver.persistence.domain.LocationCounts;
import com.revealprecision.revealserver.persistence.domain.TaskBusinessStateTracker;
import com.revealprecision.revealserver.persistence.projection.LocationBusinessStateCount;
import com.revealprecision.revealserver.persistence.repository.LocationAboveStructureCountsRepository;
import com.revealprecision.revealserver.persistence.repository.LocationCountsRepository;
import com.revealprecision.revealserver.persistence.repository.TaskBusinessStateTrackerRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationBusinessStatusService {

  private final LocationCountsRepository locationCountsRepository;
  private final TaskBusinessStateTrackerRepository taskBusinessStateTrackerRepository;
  private final LocationAboveStructureCountsRepository locationAboveStructureCountsRepository;

  public LocationCounts getLocationCountsForGeoLevelByHierarchyLocationParent(
      UUID parentLocationIdentifier, UUID locationHierarchyIdentifier, String geographicLevelName) {

    log.trace("parms passed: {} {} {}", parentLocationIdentifier, locationHierarchyIdentifier,
        geographicLevelName);
    LocationCounts locationCounts = locationCountsRepository.findLocationCountsByParentLocationIdentifierAndLocationHierarchyIdentifierAndGeographicLevelName(
        parentLocationIdentifier, locationHierarchyIdentifier, geographicLevelName);
    if (locationCounts != null) {
      log.trace("result: {} {} {} {} {} {} {} ", parentLocationIdentifier,
          locationHierarchyIdentifier, geographicLevelName,
          locationCounts.getParentLocationIdentifier(),
          locationCounts.getLocationCount(), locationCounts.getParentLocationName(),
          locationCounts.getParentGeographicLevelName());
    }
    return locationCounts;
  }


  public Set<LocationBusinessStateCount> getLocationBusinessStateObjPerGeoLevel(UUID planIdentifier,
      UUID parentLocationIdentifier, String taskLocationGeographicLevelName,
      UUID locationHierarchyIdentifier) {
    return taskBusinessStateTrackerRepository.getLocationBusinessStateObjPerGeoLevel(planIdentifier,
        parentLocationIdentifier, taskLocationGeographicLevelName, locationHierarchyIdentifier);
  }

  public LocationBusinessStateCount getLocationBusinessStateObjPerBusinessStatusAndGeoLevel(
      UUID planIdentifier, UUID parentLocationIdentifier, String taskLocationGeographicLevelName,
      String taskBusinessStatus, UUID locationHierarchyIdentifier) {
    return taskBusinessStateTrackerRepository.getLocationBusinessStateObjPerBusinessStatusAndGeoLevel(
        planIdentifier, parentLocationIdentifier, taskLocationGeographicLevelName,
        taskBusinessStatus, locationHierarchyIdentifier);
  }

  public TaskBusinessStateTracker findLocationBusinessState(UUID locationHierarchyIdentifier,
      UUID taskLocationIdentifier, UUID planIdentifier) {
    return taskBusinessStateTrackerRepository.findDistinctTaskBusinessStateTrackerByLocationHierarchyIdentifierAndTaskLocationIdentifierAndPlanIdentifier(
        locationHierarchyIdentifier, taskLocationIdentifier, planIdentifier);
  }

  public LocationAboveStructure getLocationAboveStructureCount(UUID parentLocationIdentifier,
      UUID locationHierarchyIdentifier, UUID locationAboveStructureIdentifier
      , UUID planIdentifier) {
    return locationAboveStructureCountsRepository.getLocationAboveStructureByCompositePrimaryKey(
        parentLocationIdentifier, locationHierarchyIdentifier, locationAboveStructureIdentifier
        , planIdentifier);
  }

  public Set<LocationAboveStructure> getLocationAboveStructureCounts(
      UUID locationHierarchyIdentifier, UUID locationAboveStructureIdentifier
      , UUID planIdentifier) {
    return locationAboveStructureCountsRepository.getLocationAboveStructureByPlanLocationHierarchyAndLocationAbove(
         locationHierarchyIdentifier, locationAboveStructureIdentifier
        , planIdentifier);
  }

  public void saveLocationAboveStructureList(
      List<LocationAboveStructure> locationAboveStructureList){
    locationAboveStructureCountsRepository.saveAll(locationAboveStructureList);
  }

}
