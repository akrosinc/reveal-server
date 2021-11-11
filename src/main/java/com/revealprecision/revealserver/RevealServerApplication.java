package com.revealprecision.revealserver;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RevealServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RevealServerApplication.class, args);
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
