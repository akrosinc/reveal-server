package com.revealprecision.revealserver.props;

import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "reprocess")
@Component
@Setter
@Getter
public class ReprocessEventsProperties {

  int taskBatchSize;
  int eventBatchSize;
}
