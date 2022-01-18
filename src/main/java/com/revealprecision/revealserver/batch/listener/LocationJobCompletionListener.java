package com.revealprecision.revealserver.batch.listener;

import com.revealprecision.revealserver.enums.BulkStatusEnum;
import com.revealprecision.revealserver.persistence.domain.LocationBulk;
import com.revealprecision.revealserver.persistence.repository.LocationBulkRepository;
import com.revealprecision.revealserver.service.LocationBulkService;
import com.revealprecision.revealserver.service.StorageService;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocationJobCompletionListener implements JobExecutionListener {

  private final LocationBulkService locationBulkService;
  private final LocationBulkRepository locationBulkRepository;
  private final StorageService storageService;

  @Override
  public void beforeJob(JobExecution jobExecution) {

  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    String locationBulkId = jobExecution.getJobParameters().getString("locationBulkId");
    String filePath = jobExecution.getJobParameters().getString("filePath");
    LocationBulk locationBulk = locationBulkService.findById(UUID.fromString(locationBulkId));
    locationBulk.setStatus(BulkStatusEnum.COMPLETE);
    locationBulkRepository.save(locationBulk);
    try {
      storageService.deleteFile(filePath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
