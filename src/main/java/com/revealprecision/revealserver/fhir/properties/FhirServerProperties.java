package com.revealprecision.revealserver.fhir.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fhir.server")
public class FhirServerProperties {

  private String baseURL = "http://server";
}
