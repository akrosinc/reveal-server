package com.revealprecision.revealserver.amdr.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "amdr")
@Component
@Setter
@Getter
public class AmdrProperties {

  private String cron = "* */15 * * * *";
}
