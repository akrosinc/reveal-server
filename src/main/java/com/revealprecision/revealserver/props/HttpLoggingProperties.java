package com.revealprecision.revealserver.props;

import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "http.logging")
@Component("httpLoggingProperties")
@Getter
@Setter
public class HttpLoggingProperties {

  private String cleanUpCron = "0 0/30 * * * *";

  private Integer chronoAmount = 1;
  private ChronoUnit chronoUnit = ChronoUnit.DAYS;

  private boolean shouldLogToConsole = true;
  private boolean shouldLogToDatabase = true;

  private  Integer logLength = 2000;
}
