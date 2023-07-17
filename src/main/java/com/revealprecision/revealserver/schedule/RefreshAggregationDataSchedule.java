package com.revealprecision.revealserver.schedule;

import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.LocationIdEvent;
import com.revealprecision.revealserver.persistence.domain.AggregationStaging;
import com.revealprecision.revealserver.persistence.repository.AggregationStagingRepository;
import com.revealprecision.revealserver.persistence.repository.EventAggregateRepository;
import com.revealprecision.revealserver.persistence.repository.ImportAggregateRepository;
import com.revealprecision.revealserver.props.ImportAggregationProperties;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.PublisherService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
@Profile("Refresh-Materialized-Views")
public class RefreshAggregationDataSchedule {

  private final ImportAggregateRepository importAggregateRepository;
  private final EventAggregateRepository eventAggregateRepository;
  private final AggregationStagingRepository aggregationStagingRepository;
  private final PublisherService publisherService;
  private final KafkaProperties kafkaProperties;
  private final ImportAggregationProperties importAggregationProperties;

  @Scheduled(cron = "#{importAggregationProperties.cron}")
  public void refreshImportAggregateMaterializedView() {
    log.debug("refreshing import and event aggregations");
    if (importAggregationProperties.isScheduleEnabled()) {
      importAggregateRepository.refreshImportAggregateNumericMaterializedView();
      importAggregateRepository.refreshImportAggregateStringCountMaterializedView();
      eventAggregateRepository.refreshImportAggregateNumericMaterializedView();
      eventAggregateRepository.refreshImportAggregateStringCountMaterializedView();

      List<AggregationStaging> aggregateStagingList = aggregationStagingRepository.findAll();

      aggregateStagingList.forEach(aggregationStaging -> publisherService.send(
          kafkaProperties.getTopicMap().get(KafkaConstants.EVENT_AGGREGATION_LOCATION),
          LocationIdEvent.builder()
              .uuids(List.of(aggregationStaging.getLocationIdentifier()))
              .hierarchyIdentifier(aggregationStaging.getHierarchyIdentifier())
              .nodeOrder(aggregationStaging.getNodeOrder())
              .build()));

      aggregationStagingRepository.deleteAll(aggregateStagingList);

      log.debug("refreshed import and event aggregations");
    }
  }

}
