package com.revealprecision.revealserver.props;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("task")
@Component
@Setter
@Getter
public class TaskGenerationProperties {

  private boolean generate = true;

  private boolean cancel = false;

  private boolean reactivate = false;
}
