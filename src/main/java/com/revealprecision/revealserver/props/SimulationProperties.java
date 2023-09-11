package com.revealprecision.revealserver.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "reveal.simulation")
@Component
@Setter
@Getter
public class SimulationProperties {

  private Integer fetchLocationPageSize = 5000;
  private Integer fetchParentPageSize = 2000;
}
