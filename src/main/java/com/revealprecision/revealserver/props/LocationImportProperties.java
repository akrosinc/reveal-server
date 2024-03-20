package com.revealprecision.revealserver.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "reveal.location-import")
@Component
@Setter
@Getter
public class LocationImportProperties {
  boolean allowDuplicates = false;
}
