package com.revealprecision.revealserver.api.v1.controller.querying;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.constants.FormConstants.BusinessStatus;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.messaging.TaskEventFactory;
import com.revealprecision.revealserver.messaging.message.Message;
import com.revealprecision.revealserver.messaging.message.TaskEvent;
import com.revealprecision.revealserver.persistence.domain.Event;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.props.ReprocessEventsProperties;
import com.revealprecision.revealserver.service.EventService;
import com.revealprecision.revealserver.service.FormDataProcessorService;
import com.revealprecision.revealserver.service.TaskService;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kafka-config")
@Slf4j
@RequiredArgsConstructor
public class KafkaEventManipulateController {

  private final KafkaProperties kafkaProperties;
  private final EventService eventService;
  private final TaskService taskService;
  private final FormDataProcessorService formDataProcessorService;
  private final ObjectMapper objectMapper;
  private final KafkaTemplate<String, Message> kafkaTemplate;
  private final ReprocessEventsProperties reprocessEventsProperties;

  @Value(value = "${kafka.bootstrapAddress}")
  private String kafkaBootStrapAddress;

  @Value(value = "${kafka.groupId}")
  private String kafkaGroupId;


  @GetMapping("/reprocess-events-for-report")
  public void reprocessEventsForReport() {
    processEventsAsync();
  }

  @GetMapping("/reprocess-events-for-report-and-performance")
  public void reprocessEventsForReportAndPerformance() {
    processEventsForReportAndPerformanceAsync();
  }

  @GetMapping("/reprocess-visited-tasks-for-report/{planIdentifier}")
  public void reprocessVisitedTasksForReport(@PathVariable("planIdentifier") UUID planIdentifier) {
    processVisitedTasksAsync(planIdentifier);
  }

  @GetMapping("/reprocess-tasks-for-report")
  public void reprocessTasksForReport() {
    processTasksAsync();
  }

  @GetMapping("/reprocess-single-tasks-not-same-for-report/{identifier}")
  public void reprocesssingleTasksForReport(@PathVariable("identifier") UUID identifier) {
    processTasksNotInTaskBusinessStateTrackerByIdentifierAsync(identifier);
  }

  @GetMapping("/reprocess-batch-tasks-not-same-for-report/{limit}")
  public void reprocesssingleTasksForReport(@PathVariable("limit") int limit) {
    processTasksNotInTaskBusinessStateTrackerAsync(limit);
  }


  @Async
  void processVisitedTasksAsync(UUID planIdentifier) {
    int page = 0;
    PageRequest pageRequest = PageRequest.of(page, reprocessEventsProperties.getTaskBatchSize());
    Page<Task> allTasks = taskService.getAllTasksForPlanWhereBusinessStatusNotIn(planIdentifier,
        List.of(BusinessStatus.NOT_VISITED), pageRequest);
    int totalProcessed = 0;
    while (page < allTasks.getTotalPages()) {

      allTasks.stream().map(TaskEventFactory::getTaskEventFromTask)
          .forEach(taskEvent ->
              kafkaTemplate.send(kafkaProperties.getTopicMap().get(
                  KafkaConstants.TASK), taskEvent));

      int size = allTasks.getContent().size();
      log.info("Complete page {} with {} number of rows", page, size);
      page++;
      totalProcessed = totalProcessed + size;
      pageRequest = PageRequest.of(page, reprocessEventsProperties.getTaskBatchSize());
      allTasks = taskService.getAllTasksForPlanWhereBusinessStatusNotIn(planIdentifier,
          List.of(BusinessStatus.NOT_VISITED), pageRequest);
    }
    log.info("Total tasks resent-> {}", totalProcessed);
  }


  @Async
  void processTasksAsync() {
    int page = 0;
    PageRequest pageRequest = PageRequest.of(page, reprocessEventsProperties.getTaskBatchSize());
    Page<Task> allTasks = taskService.getAllTasks(pageRequest);
    int totalProcessed = 0;
    while (page < allTasks.getTotalPages()) {

      allTasks.stream().map(TaskEventFactory::getTaskEventFromTask)
          .forEach(taskEvent ->
              kafkaTemplate.send(kafkaProperties.getTopicMap().get(
                  KafkaConstants.TASK), taskEvent));

      int size = allTasks.getContent().size();
      log.info("Complete page {} with {} number of rows", page, size);
      page++;
      totalProcessed = totalProcessed + size;
      pageRequest = PageRequest.of(page, reprocessEventsProperties.getTaskBatchSize());
      allTasks = taskService.getAllTasks(pageRequest);
    }
    log.info("Total tasks resent-> {}", totalProcessed);
  }


  @Async
  void processTasksNotInTaskBusinessStateTrackerAsync(int limit) {
    List<Task> allTasks = taskService.getAllTasksNotSameAsTaskBusinessStateTracker(limit);
    int totalProcessed = 0;

    allTasks.stream().map(Task::getIdentifier).map(taskService::getTaskByIdentifier).map(TaskEventFactory::getTaskEventFromTask)
        .forEach(taskEvent ->
            kafkaTemplate.send(kafkaProperties.getTopicMap().get(
                KafkaConstants.TASK), taskEvent));

    log.info("Completed sending tasks");
  }

  @Async
  void processTasksNotInTaskBusinessStateTrackerByIdentifierAsync(UUID identifier) {
    Task allTasks = taskService.getAllTasksNotSameAsTaskBusinessStateTrackerByIdentifier(
        identifier);

    Task taskByIdentifier = taskService.getTaskByIdentifier(allTasks.getIdentifier());

    TaskEvent taskEvent = TaskEventFactory.getTaskEventFromTask(taskByIdentifier);

    kafkaTemplate.send(kafkaProperties.getTopicMap().get(
        KafkaConstants.TASK), taskEvent);

    log.info("Completed sending tasks");
  }


  @Async
  void processEventsAsync() {
    int page = 0;
    PageRequest pageRequest = PageRequest.of(page, reprocessEventsProperties.getEventBatchSize());
    Page<Event> allEvents = eventService.getAllEvents(pageRequest);
    int totalProcessed = 0;
    while (page < allEvents.getTotalPages()) {

      for (Event event : allEvents) {
        try {
          formDataProcessorService.processFormDataAndSubmitToMessagingForReport(event);

        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      int size = allEvents.getContent().size();
      log.info("Complete page {} with {} number of rows", page, size);
      page++;
      totalProcessed = totalProcessed + size;

      pageRequest = PageRequest.of(page, reprocessEventsProperties.getEventBatchSize());
      allEvents = eventService.getAllEvents(pageRequest);
    }
    log.info("Total events resent-> {}", totalProcessed);

  }

  @Async
  void processEventsForReportAndPerformanceAsync() {
    int page = 0;
    PageRequest pageRequest = PageRequest.of(page, reprocessEventsProperties.getEventBatchSize());
    Page<Event> allEvents = eventService.getAllEvents(pageRequest);
    int totalProcessed = 0;
    while (page < allEvents.getTotalPages()) {

      for (Event event : allEvents) {
        try {
          formDataProcessorService.processFormDataAndSubmitToMessagingForReport(event);
          formDataProcessorService.processFormDataAndSubmitToMessagingTemp(event);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      int size = allEvents.getContent().size();
      log.info("Complete page {} with {} number of rows", page, size);
      page++;
      totalProcessed = totalProcessed + size;

      pageRequest = PageRequest.of(page, reprocessEventsProperties.getEventBatchSize());
      allEvents = eventService.getAllEvents(pageRequest);
    }
    log.info("Total events resent-> {}", totalProcessed);

  }
}
