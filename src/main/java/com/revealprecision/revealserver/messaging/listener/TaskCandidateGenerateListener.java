package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.enums.ProcessTrackerEnum;
import com.revealprecision.revealserver.messaging.message.TaskProcessEvent;
import com.revealprecision.revealserver.persistence.domain.ProcessTracker;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.service.ProcessTrackerService;
import com.revealprecision.revealserver.service.TaskService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("KafkaMessaging & (Listening | task-candidate-generation-listener)")
public class TaskCandidateGenerateListener extends Listener {

  private final TaskService taskService;

  private final ProcessTrackerService processTrackerService;

  private static final List<String> skipList = new ArrayList<>();

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('TASK_CANDIDATE_GENERATE')}", groupId = "reveal_server_group")
  public void listenGroupFoo(TaskProcessEvent message) {
    log.info("Received Message in group foo: {}", message.toString());
    init();

    UUID processTrackerIdentifier = message.getProcessTracker().getIdentifier();

    Optional<ProcessTracker> processTracker = processTrackerService.findByIdentifier(
        processTrackerIdentifier);

    if (processTracker.isPresent()) {
      ProcessTracker processTracker1 = processTracker.get();
      log.info(
          "TASK_CANDIDATE_GENERATE processTracker {} found with state {} for stage {} (baseEntity={}, parentTask={}, location={})",
          processTrackerIdentifier, processTracker1.getState(), message.getIdentifier(),
          message.getBaseEntityIdentifier(), message.getParentTaskIdentifier(),
          message.getLocationIdentifier());

      if (processTracker1.getState().equals(ProcessTrackerEnum.NEW) || processTracker1.getState()
          .equals(ProcessTrackerEnum.BUSY)) {
        Task task = taskService.generateTaskForTaskProcess(message);
        if (task != null) {
          log.info(
              "TASK_CANDIDATE_GENERATE created task {} for stage {} (baseEntity={}, location={})",
              task.getIdentifier(), message.getIdentifier(), message.getBaseEntityIdentifier(),
              message.getLocationIdentifier());
        } else {
          log.warn(
              "TASK_CANDIDATE_GENERATE did NOT create a task for stage {} (baseEntity={}, parentTask={}, location={}) - stage missing or not in NEW state (possible uncommitted-producer race)",
              message.getIdentifier(), message.getBaseEntityIdentifier(),
              message.getParentTaskIdentifier(), message.getLocationIdentifier());
        }
      } else {
        log.info(
            "TASK_CANDIDATE_GENERATE process request {} no longer relevant (state {}) and will be ignored",
            processTrackerIdentifier, processTracker1.getState());
      }
    } else {
      log.warn(
          "TASK_CANDIDATE_GENERATE processTracker {} NOT found for stage {} - ignoring message (possible uncommitted-producer race)",
          processTrackerIdentifier, message.getIdentifier());
    }
  }
}
