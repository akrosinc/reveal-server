package com.revealprecision.revealserver.api.v1.facade.properties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "task.facade")
@Component
@Setter @Getter
public class TasKFacadeProperties {

  Map<String,String> businessStatusMapping = Collections.singletonMap("GENERAL","business_status");

}
