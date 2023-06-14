package com.revealprecision.revealserver.api.v1.controller.querying;

import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.message.LocationIdEvent;
import com.revealprecision.revealserver.persistence.es.LocationElastic;
import com.revealprecision.revealserver.persistence.repository.LocationElasticRepository;
import com.revealprecision.revealserver.persistence.repository.LocationHierarchyRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import com.revealprecision.revealserver.props.EventAggregationProperties;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.PublisherService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reveal-elastic")
@RequiredArgsConstructor
@Slf4j
public class ElasticController {

  private final LocationElasticRepository locationElasticRepository;
  private final LocationRepository locationRepository;
  private final LocationHierarchyRepository locationHierarchyRepository;
  private final EventAggregationProperties eventAggregationProperties;
  private final PublisherService publisherService;

  private final KafkaProperties kafkaProperties;


  @GetMapping("/generate-elastic-events/{locationIdentifier}")
  private void generateEventAggregateEvents(@PathVariable("locationIdentifier") UUID locationIdentifier) {
    UUID aDefault = locationHierarchyRepository.findLocationHierarchyByName("default");
    String nodeOrder = locationHierarchyRepository.findNodeOrderByIdentifier(aDefault);

    publisherService.send(kafkaProperties.getTopicMap().get(
              KafkaConstants.EVENT_AGGREGATION_LOCATION),
          LocationIdEvent.builder()
              .hierarchyIdentifier(aDefault)
              .nodeOrder(nodeOrder)
              .uuids(List.of(locationIdentifier)).build());


    log.debug("done");
  }


  @GetMapping("/generate-elastic-events")
  private void generateEventAggregateEvents() {
    UUID aDefault = locationHierarchyRepository.findLocationHierarchyByName("default");
    String nodeOrder = locationHierarchyRepository.findNodeOrderByIdentifier(aDefault);

    int pageNumber = 0;
    int pageSize = eventAggregationProperties.getPageSize();

    Page<UUID> locationIdsPage = locationRepository.getAllLocationIdentifiers(
        PageRequest.of(pageNumber, pageSize));

    do {
      publisherService.send(kafkaProperties.getTopicMap().get(
              KafkaConstants.EVENT_AGGREGATION_LOCATION),
          LocationIdEvent.builder()
              .hierarchyIdentifier(aDefault)
              .nodeOrder(nodeOrder)
              .uuids(locationIdsPage.getContent()).build());

      pageNumber++;
      log.debug("sent item {}",pageNumber);
      locationIdsPage = locationRepository.getAllLocationIdentifiers(
          PageRequest.of(pageNumber, pageSize));

    } while (locationIdsPage.hasContent());

    log.debug("done");
  }


  @GetMapping("/{identifier}")
  private LocationElastic getItemById(@PathVariable("identifier") UUID identifier) {
    return locationElasticRepository.findById(identifier.toString())
        .orElseThrow(() -> new NotFoundException("location not found "));
  }

  @PostMapping("/{identifier}")
  private LocationElastic saveItemById(@PathVariable("identifier") UUID identifier,
      @RequestBody LocationElastic locationElastic) {

    Optional<LocationElastic> optional = locationElasticRepository.findById(identifier.toString());
    optional.ifPresent(locationElastic1 -> locationElasticRepository.save(locationElastic));
    if (optional.isPresent()) {
      return locationElasticRepository.save(locationElastic);
    } else {
      throw new NotFoundException("location not found ");
    }
  }
}


