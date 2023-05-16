package com.revealprecision.revealserver.props;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "event.aggregation")
@Setter
@Getter
@Component("eventAggregationProperties")
public class EventAggregationProperties {

  List<String> exclude = List.of("phone", "compoundhead", "village", "sprayop", "supervisor",
      "nameHoH", "team_leader", "verification-zone", "headMan");

  String cron = "0 0/30 * * * *";

  public String getExclusionListRegex() {
    return exclude.stream().map(item -> ".*".concat(item).concat(".*"))
        .collect(Collectors.joining("|"));
  }

  Integer pageSize = 200;

  String delim = "-";
}
