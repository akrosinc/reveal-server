package com.revealprecision.revealserver.schedule;

import com.revealprecision.revealserver.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class TaskProcessTrackerSchedule {

  private final TaskService taskService;


  @Scheduled(cron = "#{taskProcessTrackerScheduleProperties.trackerSchedule}")
  public void refreshAssignedStructureCountsMaterializedView() {
    log.debug("schedule start");
    taskService.updateProcessTrackers();
    log.debug("schedule end");
  }

}
