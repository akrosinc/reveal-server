package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.messaging.message.LocationRelationshipMessage;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.repository.LocationRelationshipRepository;
import com.revealprecision.revealserver.props.KafkaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationUtilService {

  private final LocationRelationshipRepository locationRelationshipRepository;
  private final PublisherService publisherService;
  private final KafkaProperties kafkaProperties;

  public void createRelationshipForRoot(Location location, LocationHierarchy locationHierarchy) {
    LocationRelationship locationRelationship = LocationRelationship.builder()
        .location(location)
        .locationHierarchy(locationHierarchy)
        .build();
    locationRelationship.setEntityStatus(EntityStatus.ACTIVE);
    locationRelationshipRepository.save(locationRelationship);

    LocationRelationshipMessage locationRelationshipMessage = new LocationRelationshipMessage();
    locationRelationshipMessage.setGeoNameLevelNumber(
        locationHierarchy.getNodeOrder().indexOf(
            location.getGeographicLevel().getName()));
    locationRelationshipMessage.setLocationIdentifier(
        location.getIdentifier());
    locationRelationshipMessage.setGeoName(
        location.getGeographicLevel().getName());
    locationRelationshipMessage.setLocationName(location.getName());
    locationRelationshipMessage.setLocationHierarchyIdentifier(
        locationHierarchy.getIdentifier());
    publisherService.send(kafkaProperties.getTopicMap().get(KafkaConstants.LOCATIONS_IMPORTED),
        locationRelationshipMessage);

  }
}
