package com.revealprecision.revealserver.persistence.domain;

import java.util.List;
import java.util.UUID;
import javax.validation.constraints.NotNull;
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
@ConfigurationProperties(prefix = "resource-planning.country")
@Component
@Validated
public class CountryCampaign{

  @NotNull
  private UUID identifier;

  @NotNull
  private String name;

  private List<AgeGroup> groups;

  @NotNull
  private String key;
}
