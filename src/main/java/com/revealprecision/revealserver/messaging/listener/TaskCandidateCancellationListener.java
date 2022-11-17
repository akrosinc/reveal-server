package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.enums.ProcessTrackerEnum;
import com.revealprecision.revealserver.messaging.message.TaskProcessEvent;
import com.revealprecision.revealserver.persistence.domain.ProcessTracker;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.service.ProcessTrackerService;
import com.revealprecision.revealserver.service.TaskService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("Listening | task-candidate-cancellation-listener")
public class TaskCandidateCancellationListener extends Listener {

  private final TaskService taskService;

  private final ProcessTrackerService processTrackerService;

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('TASK_CANDIDATE_CANCEL')}", groupId = "reveal_server_group")
  public void listenGroupFoo(TaskProcessEvent message) {
    //TODO: make this traceable - i.e. the application should know when task generation starts / ends
    log.info("Received Message in group foo: {}", message.toString());
    init();

    Optional<ProcessTracker> processTracker = processTrackerService.findByIdentifier(
        message.getProcessTracker().getIdentifier());

    if (processTracker.isPresent()) {
      ProcessTracker processTracker1 = processTracker.get();
      if (processTracker1.getState().equals(ProcessTrackerEnum.NEW) || processTracker1.getState()
          .equals(ProcessTrackerEnum.BUSY)) {
        Task task;
        taskService.cancelTask(message);
      } else {
        log.info("this process request is no longer relevant and will be ignored");
      }
    }
  }
}
