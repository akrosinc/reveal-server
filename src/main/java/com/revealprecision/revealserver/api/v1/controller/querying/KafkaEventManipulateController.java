package com.revealprecision.revealserver.api.v1.controller.querying;

import com.revealprecision.revealserver.api.v1.dto.factory.TaskUpdateFacadeFactory;
import com.revealprecision.revealserver.api.v1.facade.service.EventClientFacadeService;
import com.revealprecision.revealserver.api.v1.facade.service.TaskFacadeService;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.messaging.TaskEventFactory;
import com.revealprecision.revealserver.messaging.message.Message;
import com.revealprecision.revealserver.persistence.projection.TaskDataFromEventProjection;
import com.revealprecision.revealserver.persistence.repository.CleanupRepository;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.EventService;
import com.revealprecision.revealserver.service.FormDataProcessorService;
import com.revealprecision.revealserver.service.TaskService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kafka-config")
@Slf4j
@RequiredArgsConstructor
@Profile("cleanup")
public class KafkaEventManipulateController {

  private final KafkaProperties kafkaProperties;
  private final EventService eventService;
  private final TaskService taskService;
  private final FormDataProcessorService formDataProcessorService;
  private final KafkaTemplate<String, Message> kafkaTemplate;
  private final CleanupRepository cleanupRepository;
  private final TaskFacadeService taskFacadeService;
  private final EventClientFacadeService eventClientFacadeService;

  //Run First
  @GetMapping("/reprocess-events-for-report-and-performance")
  private void reprocessEventsForReportAndPerformance() {
    processEventsForReportAndPerformanceAsync();
  }

  //Run third
  @GetMapping("/reprocess-tasks-align-task-business-status-tracker")
  private void reprocessTasksForAlignTaskBusinessStatusTracker() {
    processTasksNotSameAsTaskBusinessStateTrackerAsync();
  }

  //Run fourth
  @GetMapping("/reprocess-tasks-not-in-task-business-status-tracker")
  private void reprocessTasksNotInTaskBusinessStateTrackers() {
    processTasksNotInTaskBusinessStateTrackerAsync();
  }


  //Run second
  @GetMapping("/align-task-to-event")
  private List<String> alignTaskToEvent() {
    return processAlignTaskToEvent();
  }

  @Async
  List<String> processAlignTaskToEvent(){
    List<TaskDataFromEventProjection> taskDataFromEventProjections =  cleanupRepository.getBusinessStatusFromEventWhereTaskDiffersFromEvent();
    if (taskDataFromEventProjections != null && !taskDataFromEventProjections.isEmpty()){
      return taskFacadeService.updateTaskStatusAndBusinessStatusForListOfTasks(taskDataFromEventProjections.stream().map(TaskUpdateFacadeFactory::fromTaskDataFromEvent).collect(
          Collectors.toList()));
    }
    return null;
  }

  @Async
  void processTasksNotSameAsTaskBusinessStateTrackerAsync() {
    List<String> allTasks = taskService.getAllTasksNotSameAsTaskBusinessStateTracker();

    allTasks.stream().map(UUID::fromString).map(taskService::getTaskByIdentifier).map(TaskEventFactory::getTaskEventFromTask)
        .forEach(taskEvent ->
            kafkaTemplate.send(kafkaProperties.getTopicMap().get(
                KafkaConstants.TASK), taskEvent));

    log.info("Completed sending tasks");
  }

  @Async
  void processTasksNotInTaskBusinessStateTrackerAsync() {
    List<String> allTasks = taskService.getAllTasksNotInTaskBusinessStateTracker();

    allTasks.stream().map(UUID::fromString).map(taskService::getTaskByIdentifier).map(TaskEventFactory::getTaskEventFromTask)
        .forEach(taskEvent ->
            kafkaTemplate.send(kafkaProperties.getTopicMap().get(
                KafkaConstants.TASK), taskEvent));

    log.info("Completed sending tasks");
  }

  @Async
  void processEventsForReportAndPerformanceAsync() {

    List<String> eventIdsWhereBusinessStatusNotMatchingToReport = cleanupRepository.getEventIdsWhereBusinessStatusNotMatchingToReport();

    List<String> eventIdsNotSentToReport = cleanupRepository.getEventIdsNotSentToReport();

    List<String> combinedEventIds = new ArrayList<>();

    if (eventIdsWhereBusinessStatusNotMatchingToReport  != null && !eventIdsWhereBusinessStatusNotMatchingToReport.isEmpty()){
      combinedEventIds.addAll(eventIdsWhereBusinessStatusNotMatchingToReport);
    }

    if (eventIdsNotSentToReport  != null && !eventIdsNotSentToReport.isEmpty()){
      combinedEventIds.addAll(eventIdsNotSentToReport);
    }

    if (!combinedEventIds.isEmpty()){
      combinedEventIds.stream().map(eventId ->
          eventService.getEventById(UUID.fromString(eventId))
          ).filter(Objects::nonNull).forEach(event -> {
        try {
          formDataProcessorService.processFormDataAndSubmitToMessaging(event,eventClientFacadeService.getEventFacade(event));
        } catch (Exception ioe) {
          log.error("cannot process event {}",event.getIdentifier(),ioe);
        }
      });
    }
  }
}
