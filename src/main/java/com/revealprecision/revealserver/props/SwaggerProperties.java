package com.revealprecision.revealserver.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("reveal.swagger")
@Setter @Getter
public class SwaggerProperties {
  private String server = "https://api-my-local.akros.digital";
}
