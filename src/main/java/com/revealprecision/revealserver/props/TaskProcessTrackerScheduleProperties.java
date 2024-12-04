package com.revealprecision.revealserver.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "reveal.task")
@Component
@Setter
@Getter
public class TaskProcessTrackerScheduleProperties {

  private String trackerSchedule = "0 0/5 * * * *";

}
