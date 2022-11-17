package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.domain.Event;
import com.revealprecision.revealserver.persistence.domain.RawEvent;
import com.revealprecision.revealserver.persistence.repository.EventRepository;
import com.revealprecision.revealserver.persistence.repository.RawEventRepository;
import com.revealprecision.revealserver.persistence.specification.EventSpec;
import com.revealprecision.revealserver.service.models.EventSearchCriteria;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventService {

  private final EventRepository eventRepository;
  private final RawEventRepository rawEventRepository;
  private final EventSpec eventSpec;

  public Event saveEvent(Event event) {
    return eventRepository.save(event);
  }

  public Page<Event> searchEvents(EventSearchCriteria eventSearchCriteria, Pageable pageable) {
    return eventRepository.findAll(eventSpec.getEventSpecification(eventSearchCriteria), pageable);
  }

  public RawEvent saveRawEvent(RawEvent rawEvent){
    return rawEventRepository.save(rawEvent);
  }

  public Event getEventById(UUID identifier){
    return eventRepository.getById(identifier);
  }
}
