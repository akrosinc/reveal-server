package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.messaging.message.TaskProcessEvent;
import com.revealprecision.revealserver.service.TaskService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskCandidateCancellationListener extends Listener {

  private final TaskService taskService;

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('TASK_CANDIDATE_CANCEL')}", groupId = "reveal_server_group")
  public void listenGroupFoo(TaskProcessEvent message) throws IOException {
    //TODO: make this traceable - i.e. the application should know when task generation starts / ends
    log.info("Received Message in group foo: {}", message.toString());
    init();

    taskService.cancelTask(message);

  }
}
