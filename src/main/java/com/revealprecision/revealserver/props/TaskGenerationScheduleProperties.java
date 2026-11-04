package com.revealprecision.revealserver.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Holds the cron expression for the recapture-limit task generation schedule.
 *
 * <p>Override with {@code reveal.task-generation.recapture-limit-schedule} in the active
 * application properties. Defaults to running once daily at midnight.</p>
 */
@ConfigurationProperties(prefix = "reveal.task-generation")
@Component
@Setter
@Getter
public class TaskGenerationScheduleProperties {

  // Second Minute Hour Day-of-month Month Day-of-week -> daily at 00:00
  private String recaptureLimitSchedule = "0 0 0 * * *";

  // Owner attributed to system-generated recapture-limit tasks. The schedule runs with no
  // authenticated principal, so tasks are attributed to this configurable system owner rather
  // than being left unowned.
  private String recaptureLimitTaskOwner = "system";

}
