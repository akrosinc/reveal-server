package com.revealprecision.revealserver.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.batch.dto.LocationValidationDTO;
import com.revealprecision.revealserver.batch.listener.LocationJobCompletionListener;
import com.revealprecision.revealserver.batch.processor.LocationValidationItemProcessor;
import com.revealprecision.revealserver.batch.writer.LocationValidationWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.builder.JsonItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.PathResource;

@RequiredArgsConstructor
@Configuration
@EnableBatchProcessing
public class LocationValidationBatchConfig {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final LocationValidationItemProcessor locationValidationProcessor;

  @Bean
  @StepScope
  public JsonItemReader<LocationRequest> locationValidationReader(
      @Value("#{jobParameters['filePath']}") String filePath) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    JacksonJsonObjectReader<LocationRequest> jsonObjectReader = new JacksonJsonObjectReader<>(LocationRequest.class);
    jsonObjectReader.setMapper(objectMapper);
    return new JsonItemReaderBuilder<LocationRequest>()
        .name("locationReader")
        .resource(new PathResource(filePath))
        .jsonObjectReader(jsonObjectReader)
        .build();
  }

  @Bean
  public Job importLocationValidationJob(LocationJobCompletionListener locationJobCompletionListener,
      Step validationStep) {
    return jobBuilderFactory.get("importLocationValidationJob")
        .flow(validationStep).end().build();
  }

  @Bean
  @StepScope
  public LocationValidationWriter locationValidationWriter(@Value("#{jobParameters['fileName']}") String fileName) {
    LocationValidationWriter locationValidationWriter = new LocationValidationWriter(fileName);
    return locationValidationWriter;
  }

  @Bean
  public Step validationStep() {
    return stepBuilderFactory.get("validationStep")
        .<LocationRequest, LocationValidationDTO>chunk(40)
        .reader(locationValidationReader(null))
        .processor(locationValidationProcessor)
        .writer(locationValidationWriter(null))
        .build();
  }
}
