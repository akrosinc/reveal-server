package com.revealprecision.revealserver.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.batch.writer.LocationWriter;
import com.revealprecision.revealserver.batch.listener.LocationJobCompletionListener;
import com.revealprecision.revealserver.batch.processor.LocationItemProcessor;
import com.revealprecision.revealserver.persistence.domain.Location;
import javax.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaItemWriter;
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
public class LocationBatchConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final EntityManagerFactory entityManagerFactory;
  private final LocationItemProcessor locationItemProcessor;

  @Bean
  @StepScope
  public JsonItemReader<LocationRequest> locationReader(
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
  public JpaItemWriter<Location> locationWriter() {
    JpaItemWriter<Location> locationWriter = new JpaItemWriter<>();
    locationWriter.setEntityManagerFactory(entityManagerFactory);
    return locationWriter;
  }

  @Bean
  public LocationWriter locationWriterTest() {
    LocationWriter locationWriter = new LocationWriter(null, null);
    return locationWriter;
  }

  @Bean
  public Job importLocationJob(LocationJobCompletionListener locationJobCompletionListener,
      Step step1) {
    return jobBuilderFactory.get("importLocationJob")
        .listener(locationJobCompletionListener)
        .flow(step1).end().build();
  }

  @Bean
  public Step step1() {
    return stepBuilderFactory.get("step1")
        .<LocationRequest, Location>chunk(10)
        .reader(locationReader(null))
        .processor(locationItemProcessor)
        .writer(locationWriterTest())
        .build();
  }

}
