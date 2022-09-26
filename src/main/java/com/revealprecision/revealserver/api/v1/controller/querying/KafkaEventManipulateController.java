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
  public void reprocessEvents() {

    List<Event> allEvents = eventService.getAllEvents();
    for (Event event:allEvents) {
      try {
        formDataProcessorService.processFormDataAndSubmitToMessagingTemp(event);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
