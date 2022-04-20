package com.revealprecision.revealserver.api.v1.facade.factory;

import com.revealprecision.revealserver.api.v1.dto.response.EventClientFacadeSyncResponse;
import com.revealprecision.revealserver.api.v1.facade.models.ClientFacade;
import com.revealprecision.revealserver.api.v1.facade.models.EventFacade;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.util.Pair;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventClientFacadeSyncResponseFactory {

  public static EventClientFacadeSyncResponse fromEventsAndClients(
      Pair<List<EventFacade>, List<ClientFacade>> eventsAndClients) {
    List<EventFacade> events = eventsAndClients.getFirst();
    List<ClientFacade> clients = eventsAndClients.getSecond();
    return EventClientFacadeSyncResponse.builder().clients(clients)
        .events(events).noOfEvents(events.size())
        .totalRecords(Long.valueOf(clients.size() + events.size())).build();
  }

}
