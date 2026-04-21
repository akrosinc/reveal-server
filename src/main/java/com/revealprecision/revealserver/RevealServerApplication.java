package com.revealprecision.revealserver;

import com.cosium.spring.data.jpa.entity.graph.repository.support.EntityGraphJpaRepositoryFactoryBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n52.jackson.datatype.jts.JtsModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

@SpringBootApplication
@EnableAsync
@EnableJpaAuditing
@Slf4j
@EnableConfigurationProperties
@ServletComponentScan
@EnableScheduling
@RequiredArgsConstructor
@EnableCaching
@EnableJpaRepositories(basePackages =
    {
        "com.revealprecision.revealserver.persistence.repository",
        "com.revealprecision.revealserver.amdr.persistence.repository"
    }
    , repositoryFactoryBeanClass = EntityGraphJpaRepositoryFactoryBean.class)
@EntityScan
public class RevealServerApplication {


  public static void main(String[] args) {
    SpringApplication.run(RevealServerApplication.class, args);
  }

  @Bean //Bean definition of component that generates Validator for Bean Validation
  static LocalValidatorFactoryBean localValidatorFactoryBean() {
    return new LocalValidatorFactoryBean();
  }

  @Bean // Method Validation(AOP)Bean definition of the component to which
  static MethodValidationPostProcessor methodValidationPostProcessor(
      LocalValidatorFactoryBean localValidatorFactoryBean) {
    MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
    processor.setValidator(localValidatorFactoryBean);
    return processor;
  }

  @Bean
  public JtsModule jtsModule() {
    return new JtsModule();
  }


}
