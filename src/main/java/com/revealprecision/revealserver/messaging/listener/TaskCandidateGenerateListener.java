package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.messaging.message.TaskProcessEvent;
import com.revealprecision.revealserver.service.TaskService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskCandidateGenerateListener extends Listener {

  private final TaskService taskService;

  private static final List<String> skipList = new ArrayList<>();

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('TASK_CANDIDATE_GENERATE')}", groupId = "reveal_server_group")
  public void listenGroupFoo(TaskProcessEvent message) throws IOException {
    log.info("Received Message in group foo: {}", message.toString());
    init();

    taskService.generateTaskForTaskProcess(message);

  }
}
