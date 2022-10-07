package com.revealprecision.revealserver.api.v1.controller.querying;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.facade.models.EventClientFacade;
import com.revealprecision.revealserver.api.v1.facade.models.EventFacade;
import com.revealprecision.revealserver.persistence.domain.Event;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.EventService;
import com.revealprecision.revealserver.service.FormDataProcessorService;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kafka-config")
@Slf4j
@RequiredArgsConstructor
public class KafkaEventManipulateController {

  private final KafkaProperties kafkaProperties;
  private final EventService eventService;
  private final FormDataProcessorService formDataProcessorService;
  private final ObjectMapper  objectMapper;

  @Value(value = "${kafka.bootstrapAddress}")
  private String kafkaBootStrapAddress;

  @Value(value = "${kafka.groupId}")
  private String kafkaGroupId;

  @GetMapping("/reprocess-events")
  public String reprocessEvents() {

    List<Event> allEvents = eventService.getAllEvents();
    for (Event event:allEvents) {
      try {
        formDataProcessorService.processFormDataAndSubmitToMessagingTemp(event);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return "ok";
  }

  @GetMapping("/reprocess-events-for-report")
  public void reprocessEventsForReport() {
    processEventsAsync();
  }

  @Async
  void processEventsAsync() {
    int page = 0;
    PageRequest pageRequest = PageRequest.of(page,2);
    Page<Event> allEvents = eventService.getAllEvents(pageRequest);
    while (page < allEvents.getTotalPages()) {

      for (Event event : allEvents) {
        try {
          formDataProcessorService.processFormDataAndSubmitToMessagingForReport(event);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      log.info("Complete page {} with {} number of rows",page,allEvents.getSize());
      page++;
      pageRequest = PageRequest.of(page,2);
      allEvents = eventService.getAllEvents(pageRequest);
    }
  }
}
