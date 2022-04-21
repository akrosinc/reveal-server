package com.revealprecision.revealserver.api.v1.facade.factory;

import com.revealprecision.revealserver.api.v1.facade.models.BaseEntity;
import com.revealprecision.revealserver.api.v1.facade.models.ClientFacade;
import com.revealprecision.revealserver.api.v1.facade.models.EventFacade;
import com.revealprecision.revealserver.api.v1.facade.response.EventClientFacadeResponse;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.util.Pair;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventClientFacadeResponseFactory {

  public static EventClientFacadeResponse fromEventsAndClients(
      Pair<List<EventFacade>, List<ClientFacade>> failedEventsAndClients) {
    return EventClientFacadeResponse.builder()
        .failedClients(
            failedEventsAndClients.getSecond().stream().map(BaseEntity::getBaseEntityId)
                .map(UUID::fromString)
                .collect(Collectors.toList()))
        .failedEvents(failedEventsAndClients.getFirst().stream().map(EventFacade::getEventId)
            .map(UUID::fromString)
            .collect(Collectors.toList())).build();
  }
}
