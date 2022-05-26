package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.api.v1.facade.models.Obs;
import com.revealprecision.revealserver.messaging.TopicConstants;
import com.revealprecision.revealserver.messaging.message.EventMetadata;
import com.revealprecision.revealserver.service.MetadataService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EventConsumptionListener {

  private final MetadataService metadataService;

  @KafkaListener(topics = TopicConstants.EVENT_CONSUMPTION, groupId = "reveal_server_group")
  public void eventConsumption(EventMetadata eventMetadata) {
    Map<String, Object> metadata = extractData(eventMetadata.getObs());
    switch (eventMetadata.getEntityPropertiesEnum()){
      case PERSON:
        if(eventMetadata.getDataType().equals("text")){
          metadataService.updatePersonMetadata(eventMetadata.getBaseEntityId(),
              metadata.get(eventMetadata.getObs().getFieldCode()),
              eventMetadata.getPlanIdentifier(),
              eventMetadata.getTaskIdentifier(),
              eventMetadata.getUser(),
              "string",
              eventMetadata.getTag(),
              "Form submission value");
        }
        break;
      case LOCATION:
        break;
    }
    System.out.println("USAO " + eventMetadata.getObs().getFieldCode());
  }

  private Map<String, Object> extractData(Obs obs){ //TODO:
    Map<String, Object> response = new HashMap<>();
    if(obs.getKeyValPairs() == null) {
      response.put(obs.getFieldCode(), obs.getValues());
    }else {
      response.put(obs.getFieldCode(), obs.getKeyValPairs().get(obs.getValues().get(0)));
    }
    return response;
  }
}
