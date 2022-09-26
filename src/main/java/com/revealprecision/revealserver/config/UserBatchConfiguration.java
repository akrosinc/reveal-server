package com.revealprecision.revealserver.config;

import com.revealprecision.revealserver.batch.BatchConstants;
import com.revealprecision.revealserver.batch.CustomSkipPolicy;
import com.revealprecision.revealserver.batch.dto.UserBatchDTO;
import com.revealprecision.revealserver.batch.listener.UserJobCompletionNotificationListener;
import com.revealprecision.revealserver.batch.mapper.UserFieldSetMapper;
import com.revealprecision.revealserver.batch.processor.UserItemProcessor;
import com.revealprecision.revealserver.persistence.domain.User;
import javax.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.PathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

//@Configuration
@RequiredArgsConstructor
//@EnableBatchProcessing
public class UserBatchConfiguration {

  private final UserFieldSetMapper userFieldSetMapper;
  private final EntityManagerFactory entityManagerFactory;
  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final UserItemProcessor userItemProcessor;
  private final JobRepository jobRepository;
  private final CustomSkipPolicy customSkipPolicy;
  private final PlatformTransactionManager platformTransactionManager;


//  @Bean
  public TaskExecutor getAsyncExecutor() {

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(7);
    executor.setMaxPoolSize(1000);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setThreadNamePrefix("Async-");
    executor.initialize();
    return new DelegatingSecurityContextAsyncTaskExecutor(
        executor);
  }

//  @Bean
  public TaskExecutor getAsyncExecutorTest() {

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(25);
    executor.setMaxPoolSize(1000);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setThreadNamePrefix("AsyncTest-");
    executor.initialize();
    return new DelegatingSecurityContextAsyncTaskExecutor(
        executor);
  }

//  @Bean
  public JobLauncher asyncJobLauncher() throws Exception {
    SimpleJobLauncher jobLauncher = new SimpleJobLauncher();

    jobLauncher.setJobRepository(jobRepository);
    jobLauncher.setTaskExecutor(getAsyncExecutor());
    return jobLauncher;
  }

//  @Bean
  public LineMapper<UserBatchDTO> userLineMapper() {
    DefaultLineMapper<UserBatchDTO> lineMapper = new DefaultLineMapper<>();
    lineMapper.setLineTokenizer(new DelimitedLineTokenizer() {
      {
        setNames(BatchConstants.userFields);
      }
    });
    lineMapper.setFieldSetMapper(userFieldSetMapper);

    return lineMapper;
  }

//  @Bean
//  @StepScope
//  public FlatFileItemReader<UserBatchDTO> userReader(
//      @Value("#{jobParameters['filePath']}") String filePath) {
//    return new FlatFileItemReaderBuilder<UserBatchDTO>().name("userReader")
//        .resource(new PathResource(filePath))
//        .lineMapper(userLineMapper())
//        .linesToSkip(1)
//        .build();
//  }

//  @Bean
  public JpaItemWriter<User> userWriter() {
    JpaItemWriter<User> writer = new JpaItemWriter<>();
    writer.setEntityManagerFactory(entityManagerFactory);
    return writer;
  }

//  @Bean
  public Job importUserJob(UserJobCompletionNotificationListener jobListener, Step userStep) {
    return jobBuilderFactory.get("importUserJob")
        .listener(jobListener)
        .flow(userStep)
        .end()
        .build();
  }

//  @Bean
  public Step userStep() {
    return stepBuilderFactory.get("step1")
        .<UserBatchDTO, User>chunk(10)
//        .reader(userReader(null))
        .processor(userItemProcessor)
        .writer(userWriter())
        .faultTolerant()
        .skipPolicy(customSkipPolicy)
        .taskExecutor(getAsyncExecutor())
        .throttleLimit(20)
        .build();
  }
}
