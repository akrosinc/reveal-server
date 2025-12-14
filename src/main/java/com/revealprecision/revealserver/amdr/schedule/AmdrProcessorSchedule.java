package com.revealprecision.revealserver.amdr.schedule;

import com.revealprecision.revealserver.amdr.service.AmdrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class AmdrProcessorSchedule {

  private final AmdrService amdrService;

  @Scheduled(cron = "#{amdrProperties.cron}")
  public void processData() {
    log.debug("schedule start");
    amdrService.processData();
    log.debug("schedule end");
  }

}
