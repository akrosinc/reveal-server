package com.revealprecision.revealserver.fhir.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fhir.server")
@Setter
@Getter
@Profile("FHIR")
public class FhirServerProperties {

  private String baseURL = "http://localhost:8080";
  private String fhirPath = "/fhir";
}
