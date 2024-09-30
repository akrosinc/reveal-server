package com.revealprecision.revealserver.props;

import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "hdss")
@Setter @Getter
public class HdssProperties {

  private Map<UUID,UUID> target;

  private String defaultEmailList;

  private boolean sendToOverrideEmail = true;

  private String overrideEmailList;
}
