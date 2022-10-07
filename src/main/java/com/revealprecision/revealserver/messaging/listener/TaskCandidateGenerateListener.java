package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.messaging.message.TaskProcessEvent;
import com.revealprecision.revealserver.persistence.domain.Task;
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

//  private static final List<String> skipList = List.of(
//      "a1187872-696f-4b92-9dcd-6667a5d88743",
//      "aa21d729-4329-41e4-aa0c-fe80cf5ba0e6",
//      "aedfc910-090f-45ee-b7d5-ded8eb93b61a",
//      "b0e072e7-7825-46be-8168-66256cafc67e",
//      "b89ebc40-3317-40d2-8910-34ca8a1d6362",
//      "c93fed4e-f526-4676-b6b1-b7e57f3aeb74",
//      "d38014f4-e6ec-4f3e-9bd4-304dbba6d8d3",
//      "e53134bd-fc7b-4f1f-8942-bc5eeda0b556",
//      "ee6eb968-6389-4832-a85a-cc789f7a5c82"
//      );

  private static final List<String> skipList = new ArrayList<>();

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('TASK_CANDIDATE_GENERATE')}", groupId = "reveal_server_group")
  public void listenGroupFoo(TaskProcessEvent message) throws IOException {
    //TODO: make this traceable - i.e. the application should know when task generation starts / ends
    log.info("Received Message in group foo: {}", message.toString());
    init();


    if (!skipList.contains(message.getBaseEntityIdentifier().toString())) {
      taskService.generateTaskForTaskProcess(message);
    } else {
      log.info("Skipping structure as it is in the skip list: {}",message.getBaseEntityIdentifier());
    }
  }
}
