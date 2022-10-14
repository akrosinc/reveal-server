package com.revealprecision.revealserver.api.v1.facade.controller;


import static com.revealprecision.revealserver.api.v1.facade.controller.TaskFacadeController.TOTAL_RECORDS;
import static com.revealprecision.revealserver.constants.EventClientConstants.CLIENTS;
import static com.revealprecision.revealserver.constants.EventClientConstants.EVENTS;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.revealprecision.revealserver.api.v1.dto.response.EventClientFacadeSyncResponse;
import com.revealprecision.revealserver.api.v1.facade.factory.EventClientFacadeResponseFactory;
import com.revealprecision.revealserver.api.v1.facade.factory.EventClientFacadeSyncResponseFactory;
import com.revealprecision.revealserver.api.v1.facade.models.ClientFacade;
import com.revealprecision.revealserver.api.v1.facade.models.EventFacade;
import com.revealprecision.revealserver.api.v1.facade.models.SyncParamFacade;
import com.revealprecision.revealserver.api.v1.facade.response.EventClientFacadeResponse;
import com.revealprecision.revealserver.api.v1.facade.service.EventClientFacadeService;
import com.revealprecision.revealserver.persistence.domain.RawEvent;
import com.revealprecision.revealserver.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/event")
@RequiredArgsConstructor
@Slf4j
public class EventFacadeController {

  private final EventClientFacadeService eventClientFacadeService;
  private final EventService eventService;

  @Operation(summary = "Add new Events and Client objects originating from Android client",
      description = "Add new Events and Client objects originating from Android client",
      tags = {"EventClient"})
  @PostMapping(value = "/add", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<EventClientFacadeResponse> eventAdd(
      @RequestBody String eventsClientsRequest) throws JSONException, JsonProcessingException {

    try {
      eventService.saveRawEvent(RawEvent.builder()
          .captureDatetime(LocalDateTime.now())
          .eventString(eventsClientsRequest)
          .build());
    } catch (Exception e){
      log.error("Error saving raw event: {}");
    }

    JSONObject eventsClientsRequestJSON = new JSONObject(eventsClientsRequest);
    if (!eventsClientsRequestJSON.has(EVENTS) && !eventsClientsRequestJSON.has(CLIENTS)) {
      return new ResponseEntity<>(BAD_REQUEST);
    }
    Pair<List<EventFacade>, List<ClientFacade>> failedEventsAndClients = eventClientFacadeService
        .processEventsClientsRequest(eventsClientsRequestJSON);
    if (failedEventsAndClients.getFirst().isEmpty() && failedEventsAndClients.getSecond()
        .isEmpty()) {
      return new ResponseEntity<>(CREATED);
    } else {
      return ResponseEntity.status(CREATED)
          .body(EventClientFacadeResponseFactory.fromEventsAndClients(failedEventsAndClients));
    }
  }

  @Operation(summary = "Sync down Events and Client objects to Android client",
      description = "Sync down Events and Client objects to Android client",
      tags = {"EventClient"})
  @PostMapping(value = "/sync", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<EventClientFacadeSyncResponse> getEventClients(
      @RequestBody SyncParamFacade syncParam) {
    if (syncParam.getTeam() != null || syncParam.getProviderId() != null
        || syncParam.getLocationId() != null || syncParam.getBaseEntityId() != null
        || syncParam.getTeamId() != null) {
      Pair<List<EventFacade>, List<ClientFacade>> eventsAndClients = eventClientFacadeService
          .getSyncedEventsAndClients(syncParam);
      EventClientFacadeSyncResponse eventClientFacadeSyncResponse = EventClientFacadeSyncResponseFactory
          .fromEventsAndClients(eventsAndClients);
      if (syncParam.isReturnCount()) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(TOTAL_RECORDS, String.valueOf(eventClientFacadeSyncResponse.getTotalRecords()));
        return ResponseEntity.ok().headers(headers).body(eventClientFacadeSyncResponse);
      } else {
        return ResponseEntity.ok().body(eventClientFacadeSyncResponse);
      }
    } else {
      return new ResponseEntity<>(BAD_REQUEST);
    }
  }


}
