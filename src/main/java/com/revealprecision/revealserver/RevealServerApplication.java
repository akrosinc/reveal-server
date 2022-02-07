package com.revealprecision.revealserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

@SpringBootApplication
@EnableAsync
@EnableJpaAuditing
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
}
