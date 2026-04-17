package com.revealprecision.revealserver.config;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
//public class FlywayConfig {
//
//  @ConditionalOnProperty(value = "spring.flyway.custom.repair-on-migrate", havingValue = "true")
//  @Bean
//  public FlywayMigrationStrategy cleanMigrateStrategy() {
//    return flyway -> {
//      flyway.repair();
//      flyway.migrate();
//    };
//  }
//
//}
