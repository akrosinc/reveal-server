package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.response.EntityTagResponse;
import com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods;
import com.revealprecision.revealserver.constants.EntityTagDataTypes;
import com.revealprecision.revealserver.persistence.projection.EventAggregationNumericTagProjection;
import com.revealprecision.revealserver.persistence.repository.EventAggregateRepository;
import com.revealprecision.revealserver.props.EventAggregationProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventAggregationService {

  private final EventAggregateRepository eventAggregateRepository;
  private final EventAggregationProperties eventAggregationProperties;

  List<String> uniqueTagsFromEventAggregationNumeric = new ArrayList<>();
  List<String> uniqueTagsFromEventAggregationStringCount = new ArrayList<>();

  public List<EntityTagResponse> getEventBasedTags() {

    List<EntityTagResponse> entityTagResponses = uniqueTagsFromEventAggregationNumeric.stream().map(s ->
            getEntityTagResponse(
                s,
                EntityTagDataTypes.DOUBLE))
            .collect(Collectors.toList());

        entityTagResponses.addAll(uniqueTagsFromEventAggregationStringCount.stream().map(s ->
        getEntityTagResponse(
            s,
            EntityTagDataTypes.DOUBLE)
    ).collect(Collectors.toList()));

    return entityTagResponses.stream()
        .filter(entityTagResponse -> !entityTagResponse.getTag()
            .matches(eventAggregationProperties.getExclusionListRegex()))
        .collect(Collectors.toList());

  }



  public List<String> getUniqueTagsFromEventAggregationNumeric(){
   return eventAggregateRepository.getUniqueTagsFromEventAggregationNumeric()
        .stream().flatMap(eventAggregationNumericTagProjection -> {
      List<String> tags = new ArrayList<>();
      tags.add(getTagString(eventAggregationNumericTagProjection, EntityTagDataAggregationMethods.SUM));
      tags.add(getTagString(eventAggregationNumericTagProjection,EntityTagDataAggregationMethods.AVERAGE));
      tags.add(getTagString(eventAggregationNumericTagProjection,EntityTagDataAggregationMethods.MEDIAN));

      return tags.stream();

    }).collect(Collectors.toList());
  }

  public List<String> getUniqueTagsFromEventAggregationStringCount(){
    return eventAggregateRepository.getUniqueTagsFromEventAggregationStringCount()
        .stream().flatMap(eventAggregationNumericTagProjection -> {
          List<String> tags = new ArrayList<>();
          tags.add(getTagString(eventAggregationNumericTagProjection,EntityTagDataAggregationMethods.COUNT));
          return tags.stream();

        }).collect(Collectors.toList());
  }

  private String getTagString(EventAggregationNumericTagProjection eventAggregationNumericTagProjection, String agg) {
    return eventAggregationNumericTagProjection.getEventType().concat(
        eventAggregationProperties.getDelim()).concat(
        eventAggregationNumericTagProjection.getFieldCode()).concat(
        eventAggregationProperties.getDelim()).concat(agg);
  }

  @Async
  public void syncTags() {
    sync();
  }

  private void sync() {
    log.info("Syncing tags - Start");
    uniqueTagsFromEventAggregationNumeric = getUniqueTagsFromEventAggregationNumeric();
    uniqueTagsFromEventAggregationStringCount = getUniqueTagsFromEventAggregationStringCount();
    log.info("Syncing tags - Complete");
  }

  private EntityTagResponse getEntityTagResponse(
      String tagName, String dataTypes) {
    return EntityTagResponse.builder()
        .simulationDisplay(false)
        .isAggregate(true)
        .fieldType("tag")
        .valueType(dataTypes)
        .identifier(UUID.randomUUID().toString())
        .tag(tagName)
        .build();
  }
}
