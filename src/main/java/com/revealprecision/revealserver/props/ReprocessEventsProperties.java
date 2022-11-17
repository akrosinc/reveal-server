package com.revealprecision.revealserver.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "reprocess")
@Component
@Setter
@Getter
@Profile("cleanup")
public class ReprocessEventsProperties {

  int taskBatchSize;
  int eventBatchSize;
}