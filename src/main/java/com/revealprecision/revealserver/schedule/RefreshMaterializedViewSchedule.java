package com.revealprecision.revealserver.schedule;

import com.revealprecision.revealserver.service.AssignedStructureService;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
@Profile("Running | Refresh-Materialized-Views")
public class RefreshMaterializedViewSchedule {

  private final LocationRelationshipService locationRelationshipService;
  private final AssignedStructureService assignedStructureService;

  @Scheduled(cron = "#{refreshMaterializedViewProperties.assignedStructureCounts}")
  public void refreshAssignedStructureCountsMaterializedView() {
    log.debug("schedule start");
    assignedStructureService.refreshAssignedStructureCountsMaterializedView();
    log.debug("schedule end");
  }

  @Scheduled(cron = "#{refreshMaterializedViewProperties.locationCounts}")
  public void refreshLocationCountsView() {
    log.debug("schedule start");
    locationRelationshipService.refreshLocationCountsView();
    log.debug("schedule end");
  }

  @Scheduled(cron = "#{refreshMaterializedViewProperties.liteStructureCounts}")
  public void refreshLiteStructureCountView() {
    log.debug("schedule start");
    locationRelationshipService.refreshLiteStructureCountView();
    log.debug("schedule end");
  }

  @Scheduled(cron = "#{refreshMaterializedViewProperties.locationRelationships}")
  public void refreshLocationRelationshipMaterializedView() {
    log.debug("schedule start");
    locationRelationshipService.refreshLocationRelationshipMaterializedView();
    log.debug("schedule end");
  }

}
