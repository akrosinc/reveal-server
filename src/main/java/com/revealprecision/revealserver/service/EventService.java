package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Event;
import com.revealprecision.revealserver.persistence.domain.Event.Fields;
import com.revealprecision.revealserver.persistence.repository.EventRepository;
import com.revealprecision.revealserver.persistence.specification.EventSpec;
import com.revealprecision.revealserver.service.models.EventSearchCriteria;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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

  public Page<Event> searchEvents(EventSearchCriteria eventSearchCriteria, Pageable pageable) {
    return eventRepository.findAll(eventSpec.getEventSpecification(eventSearchCriteria), pageable);
  }


}
