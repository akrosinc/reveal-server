package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Event;
import com.revealprecision.revealserver.persistence.domain.Event.Fields;
import com.revealprecision.revealserver.persistence.repository.EventRepository;
import com.revealprecision.revealserver.persistence.specification.EventSpec;
import com.revealprecision.revealserver.service.models.EventSearchCriteria;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventService {

  private final EventRepository eventRepository;
  private final EventSpec eventSpec;

  public Event saveEvent(Event event) {
    return eventRepository.save(event);
  }

  public Event findEventByIdentifier(UUID eventIdentifier) {
    return eventRepository.findById(eventIdentifier).orElseThrow(
        () -> new NotFoundException(Pair.of(Fields.identifier, eventIdentifier), Event.class));
  }

  public Page<Event> searchEvents(EventSearchCriteria eventSearchCriteria, Pageable pageable) {
    return eventRepository.findAll(eventSpec.getEventSpecification(eventSearchCriteria), pageable);
  }

}
