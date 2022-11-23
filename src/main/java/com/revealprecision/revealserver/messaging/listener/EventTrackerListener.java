package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.api.v1.dto.factory.EventTrackerFactory;
import com.revealprecision.revealserver.messaging.message.EventTrackerMessage;
import com.revealprecision.revealserver.persistence.domain.EventTracker;
import com.revealprecision.revealserver.persistence.repository.EventTrackerRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventTrackerListener extends Listener {

  private final EventTrackerRepository eventTrackerRepository;

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('EVENT_TRACKER')}", groupId = "reveal_server_group")
  public void etl(EventTrackerMessage eventTrackerMessage) {
    log.info("Received Message in group foo: {}", eventTrackerMessage.toString());

    Optional<EventTracker> eventTrackerOptional = eventTrackerRepository.findEventTrackerByAggregationKey(
        eventTrackerMessage.getAggregationKey());
    EventTracker eventTracker;
    if (eventTrackerOptional.isPresent()){
      eventTracker = EventTrackerFactory.updateEntity(eventTrackerOptional.get(),eventTrackerMessage);
    } else {
      eventTracker = EventTrackerFactory.getEntity(eventTrackerMessage);
    }
    eventTrackerRepository.save(eventTracker);
  }

}
