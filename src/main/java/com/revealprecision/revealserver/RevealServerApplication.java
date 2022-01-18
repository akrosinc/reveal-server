package com.revealprecision.revealserver;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

@SpringBootApplication
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

  @Bean
  public OpenAPI customOpenApi(@Value("${springdoc.version}") String appVersion) {

    return new OpenAPI()
        .info(new Info()
            .title("Reveal Server")
            .version(appVersion)
            .description("Reveal Server forms the backend processing of the Reveal Platform")
            .license(new License()
                .name("Reveal Precision")
                .url("https://www.revealprecision.com"))
            .contact(new Contact()
                .email("info@akros.com")
                .name("Akros Inc.")
                .url("https://www.akros.com"))
        );

  }

}
