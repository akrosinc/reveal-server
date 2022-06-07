package com.revealprecision.revealserver.schedule;

import com.revealprecision.revealserver.service.MonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
@Profile("Monitor-Logging")
public class MonitorSchedule {

  private final MonitorService monitorService;

  @Scheduled(cron = "#{monitorProperties.printSchedule}")
  public void cleanDBLogs() {
    log.debug("schedule start");
    monitorService.print();
    log.debug("schedule end");
  }
}
