package com.revealprecision.revealserver.props;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "reveal.monitor")
@Component
@Setter
@Getter
public class MonitorProperties {
  private List<String> metrics = List.of(
      "process.cpu.usage",
      "system.cpu.count",
      "system.cpu.usage",
      "jvm.buffer.memory.used",
      "jvm.memory.max",
      "jvm.memory.used",
      "hikaricp.connections.idle",
      "hikaricp.connections.active",
      "hikaricp.connections.usage"
  );

  private String printSchedule = "0 0/3 * * * *";
}
