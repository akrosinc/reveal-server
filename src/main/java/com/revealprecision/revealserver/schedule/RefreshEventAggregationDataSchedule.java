package com.revealprecision.revealserver.schedule;

import com.revealprecision.revealserver.service.EventAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
@Profile("Refresh-Materialized-Views")
public class RefreshEventAggregationDataSchedule {

  private final EventAggregationService eventAggregationService;

  @Scheduled(cron = "#{eventAggregationProperties.cron}")
  public void syncTagNames() {
    log.debug("syncing syncTagNames");
    eventAggregationService.syncTags();
    log.debug("synced syncTagNames");
  }

}
