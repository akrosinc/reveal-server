package com.revealprecision.revealserver.props;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("reveal.system.override")
@Component
@Setter
@Getter
public class SystemOverrideProperties {
  boolean updateTaskFromEventData = false;
}
