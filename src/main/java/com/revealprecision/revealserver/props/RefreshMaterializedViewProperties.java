package com.revealprecision.revealserver.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "reveal.refresh-materialized-view")
@Component
@Setter
@Getter
public class RefreshMaterializedViewProperties {

  private String locationCounts = "0 0/5 * * * *";
  private String liteStructureCounts = "0 0/5 * * * *";
  private String locationRelationships = "0 0/5 * * * *";
  private String assignedStructureCounts = "0 0/5 * * * *";

}
