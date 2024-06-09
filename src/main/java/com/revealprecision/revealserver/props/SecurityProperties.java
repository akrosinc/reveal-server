package com.revealprecision.revealserver.props;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "reveal")
@Component
@Validated
public class SecurityProperties {
  private List<String> adminSecurityGroups = List.of("tech_team");

  private String tagAccessOverride = "tag_access_override";
}
