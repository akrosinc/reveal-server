package com.revealprecision.revealserver.batch.listener;

import com.revealprecision.revealserver.enums.BulkStatusEnum;
import com.revealprecision.revealserver.persistence.domain.UserBulk;
import com.revealprecision.revealserver.persistence.repository.UserBulkRepository;
import com.revealprecision.revealserver.service.StorageService;
import com.revealprecision.revealserver.service.UserBulkService;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserJobCompletionNotificationListener implements JobExecutionListener {

  private final UserBulkService userBulkService;
  private final UserBulkRepository userBulkRepository;
  private final StorageService storageService;

  @Override
  public void beforeJob(JobExecution jobExecution) {

  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    String userBulkId = jobExecution.getJobParameters().getString("userBulkId");
    String filePath = jobExecution.getJobParameters().getString("filePath");
    UserBulk userBulk = userBulkService.findById(UUID.fromString(userBulkId));
    userBulk.setStatus(BulkStatusEnum.COMPLETE);
    userBulkRepository.save(userBulk);
    try {
      storageService.deleteFile(filePath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
