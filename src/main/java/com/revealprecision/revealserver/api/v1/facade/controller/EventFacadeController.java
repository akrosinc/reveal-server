package com.revealprecision.revealserver.api.v1.facade.controller;


import static com.revealprecision.revealserver.api.v1.facade.controller.TaskFacadeController.TOTAL_RECORDS;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.response.EventClientFacadeSyncResponse;
import com.revealprecision.revealserver.api.v1.facade.factory.EventSearchCriteriaFactory;
import com.revealprecision.revealserver.api.v1.facade.models.BaseEntity;
import com.revealprecision.revealserver.api.v1.facade.models.ClientFacade;
import com.revealprecision.revealserver.api.v1.facade.models.EventFacade;
import com.revealprecision.revealserver.api.v1.facade.models.SyncParamFacade;
import com.revealprecision.revealserver.api.v1.facade.response.EventClientFacadeResponse;
import com.revealprecision.revealserver.api.v1.facade.service.EventClientFacadeService;
import com.revealprecision.revealserver.persistence.domain.Event;
import com.revealprecision.revealserver.service.models.EventSearchCriteria;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
public class EventFacadeController {

  public static final String EVENTS = "events";
  public static final String CLIENTS = "clients";
  private final EventClientFacadeService eventClientFacadeService;


  @PostMapping(value = "/add", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<EventClientFacadeResponse> eventAdd(
      @RequestBody String eventClientRequest) throws JSONException, JsonProcessingException {

    JSONObject eventClientRequestJSON = new JSONObject(eventClientRequest);
    if (!eventClientRequestJSON.has(EVENTS) && !eventClientRequestJSON.has(CLIENTS)) {
      return new ResponseEntity<>(BAD_REQUEST);
    }
    ObjectMapper mapper = new ObjectMapper();
    List<EventFacade> failedEvents = new ArrayList<>();
    if (eventClientRequestJSON.has(EVENTS)) {
      String rawEventsRequest = eventClientRequestJSON.getString(EVENTS);
      List<EventFacade> eventFacades = List
          .of(mapper.readValue(rawEventsRequest, EventFacade[].class));
      failedEvents = eventClientFacadeService.addOrUpdateEvents(eventFacades);
    }
    List<ClientFacade> failedClients = new ArrayList<>();
    if (eventClientRequestJSON.has(CLIENTS)) {
      String rawClientsRequest = eventClientRequestJSON.getString(CLIENTS);
      List<ClientFacade> clientFacades = List
          .of(mapper.readValue(rawClientsRequest, ClientFacade[].class));
      failedClients = eventClientFacadeService.addOrUpdateClients(clientFacades);
    }

    if (failedEvents.isEmpty() && failedClients.isEmpty()) {
      return new ResponseEntity<>(CREATED);
    } else {
      return ResponseEntity.status(CREATED).body(EventClientFacadeResponse.builder()
          .failedClients(
              failedClients.stream().map(BaseEntity::getBaseEntityId).map(UUID::fromString)
                  .collect(Collectors.toList()))
          .failedEvents(failedEvents.stream().map(EventFacade::getEventId).map(UUID::fromString)
              .collect(Collectors.toList())).build());
    }
  }

  @PostMapping(value = "/sync", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> getLocations(@RequestBody SyncParamFacade syncParam) {
    if (syncParam.getTeam() != null || syncParam.getProviderId() != null
        || syncParam.getLocationId() != null || syncParam.getBaseEntityId() != null
        || syncParam.getTeamId() != null) {

      EventClientFacadeSyncResponse eventClientFacadeSyncResponse = getEventClientFacadeSyncResponse(
          syncParam);

      if (syncParam.isReturnCount()) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(TOTAL_RECORDS, String.valueOf(eventClientFacadeSyncResponse.getTotalRecords()));
        return ResponseEntity.ok().headers(headers).body(eventClientFacadeSyncResponse);
      } else {
        return ResponseEntity.ok().body(eventClientFacadeSyncResponse);
      }

    } else {
      return ResponseEntity.badRequest().body("specify atleast one filter");
    }
  }

  private EventClientFacadeSyncResponse getEventClientFacadeSyncResponse(
      SyncParamFacade syncParam) {
    EventSearchCriteria eventSearchCriteria = EventSearchCriteriaFactory.getEventSearchCriteria(
        syncParam);
    PageRequest pageRequest = PageRequest.of(0, syncParam.getLimit());

    Page<Event> searchEvents = eventClientFacadeService.searchEvents(eventSearchCriteria,
        pageRequest);

    List<Event> events = eventClientFacadeService.findLatestCaptureDatePerIdentifier(
        searchEvents.get().collect(Collectors.toList()));

    List<EventFacade> eventFacades = eventClientFacadeService.getEventFacades(events);

    List<ClientFacade> combinedClientFacade = eventClientFacadeService.getClientFacades(events);

    return EventClientFacadeSyncResponse.builder().clients(combinedClientFacade)
        .events(eventFacades).noOfEvents(eventFacades.size())
        .totalRecords((long) (combinedClientFacade.size() + eventFacades.size())).build();
  }


}
