package com.revealprecision.revealserver.api.v1.facade.properties;

import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "task.facade")
@Component
@Setter @Getter
public class TaskFacadeProperties {

  Map<String,String> businessStatusMapping = Collections.singletonMap("GENERAL","business_status");

}
