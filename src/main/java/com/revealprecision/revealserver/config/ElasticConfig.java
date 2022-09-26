package com.revealprecision.revealserver.config;

//import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import org.springframework.data.elasticsearch.client.ClientConfiguration;
//import org.springframework.data.elasticsearch.client.RestClients;
//import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
//import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
//import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

//@Configuration
//@EnableElasticsearchRepositories(basePackages = "com.revealprecision.revealserver.persistence.repository")
public class ElasticConfig {

//  @Value(value = "${elasticsearch.bootstrapAddress}")
  private String bootstrapAddress;

//  @Bean
//  public RestHighLevelClient client() {
//    ClientConfiguration clientConfiguration
//        = ClientConfiguration.builder()
//        .connectedTo(bootstrapAddress)
//        .withConnectTimeout(0)
//        .withSocketTimeout(0)
//        .build();
//
//    return RestClients.create(clientConfiguration).rest();
//  }

//  @Bean
//  public ElasticsearchOperations elasticsearchTemplate() {
//    return new ElasticsearchRestTemplate(client());
//  }
}