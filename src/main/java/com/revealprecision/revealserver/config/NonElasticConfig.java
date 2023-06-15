package com.revealprecision.revealserver.config;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@Profile("!Elastic")
@Configuration
public class NonElasticConfig {

  @Bean
  public RestHighLevelClient client() {
    return null;
  }

  @Bean
  public RestClient restClient(){
    return null;
  }

  @Bean
  public ElasticsearchOperations elasticsearchTemplate() {
    return null;
  }
}