package com.revealprecision.revealserver.schedule;

import com.revealprecision.revealserver.service.HttpLoggingService;
import com.revealprecision.revealserver.service.logging.HttpDBHttpLoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
@Profile("Http-DB-Logging")
public class HttpDBLoggingCleanupSchedule {

  private final HttpLoggingService httpLoggingService;

  @Scheduled(cron = "#{httpLoggingProperties.cleanUpCron}")
  public void cleanDBLogs() {
    log.debug("schedule start");
    ((HttpDBHttpLoggingService) httpLoggingService).cleanUpLog();
    log.debug("schedule end");
  }

}
