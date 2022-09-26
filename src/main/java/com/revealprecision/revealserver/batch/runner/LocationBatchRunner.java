package com.revealprecision.revealserver.batch.runner;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

//@Service
public class LocationBatchRunner {

  private JobLauncher jobLauncher;
  private Job importLocationJob;

//  @Autowired
  public LocationBatchRunner(Job importLocationJob, @Qualifier("asyncJobLauncher") JobLauncher jobLauncher) {
    this.importLocationJob = importLocationJob;
    this.jobLauncher = jobLauncher;
  }

  public void run(String batchIdentifier, String filePath)
      throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {

    JobParameters jobParameters = new JobParametersBuilder()
        .addString("locationBulkId", batchIdentifier)
        .addString("filePath", filePath)
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();
    jobLauncher.run(importLocationJob, jobParameters);

  }
}
