package com.revealprecision.revealserver.batch.listener;

import com.revealprecision.revealserver.enums.BulkStatusEnum;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationBulk;
import com.revealprecision.revealserver.persistence.es.LocationElastic;
import com.revealprecision.revealserver.persistence.repository.LocationBulkRepository;
import com.revealprecision.revealserver.service.LocationBulkService;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.StorageService;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocationJobCompletionListener implements JobExecutionListener {

  private final LocationBulkService locationBulkService;
  private final LocationBulkRepository locationBulkRepository;
  private final StorageService storageService;
  private final LocationRelationshipService locationRelationshipService;
  private final ElasticsearchOperations elasticsearchOperations;


  @Override
  public void beforeJob(JobExecution jobExecution) {
    boolean withMapping;

    if (elasticsearchOperations.indexOps(LocationElastic.class).exists()) {
      elasticsearchOperations.indexOps(LocationElastic.class).delete();
      withMapping = elasticsearchOperations.indexOps(LocationElastic.class)
          .createWithMapping();
    } else {
      withMapping = elasticsearchOperations.indexOps(LocationElastic.class)
          .createWithMapping();
    }

    log.info("mapping created? :{}", withMapping);

  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    String locationBulkId = jobExecution.getJobParameters().getString("locationBulkId");
    String filePath = jobExecution.getJobParameters().getString("filePath");
    LocationBulk locationBulk = locationBulkService.findById(UUID.fromString(locationBulkId));


    List<Location> addedLocations = locationBulkRepository.getAllCreatedInBulk(
        locationBulk.getIdentifier());
    log.info("addLocations size: {}",addedLocations.size());
    addedLocations.forEach(location -> {
      try {
        locationRelationshipService.createRelationshipForImportedLocation(location);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    locationBulk.setStatus(BulkStatusEnum.COMPLETE);
    locationBulkRepository.save(locationBulk);

    try {
      storageService.deleteFile(filePath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
