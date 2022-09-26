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
import org.springframework.stereotype.Service;

//@Service
public class LocationValidationRunner {

  private JobLauncher jobLauncher;
  private Job importLocationValidationJob;

//  @Autowired
//  public LocationValidationRunner(Job importLocationValidationJob, JobLauncher jobLauncher) {
//    this.importLocationValidationJob = importLocationValidationJob;
//    this.jobLauncher = jobLauncher;
//  }

  public void run(String filePath, String fileName)
      throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
    System.out.println(fileName);
    JobParameters jobParameters = new JobParametersBuilder()
        .addString("filePath", filePath)
        .addString("fileName", fileName)
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();
    jobLauncher.run(importLocationValidationJob, jobParameters);

  }
}
