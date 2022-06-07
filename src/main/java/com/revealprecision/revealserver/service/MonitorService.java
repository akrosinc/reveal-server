package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.props.MonitorProperties;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint.MetricResponse;
import org.springframework.boot.actuate.metrics.MetricsEndpoint.Sample;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class MonitorService {

  private final MetricsEndpoint metricsEndpoint;

  private final MonitorProperties monitorProperties;

  private final Logger monitorLog = LoggerFactory.getLogger("monitor-file");

  public void print() {
    monitorProperties.getMetrics().forEach(metric -> {
      MetricResponse metricResponse = metricsEndpoint.metric(metric, null);
      monitorLog.debug("{} - {}", metric, metricResponse.getMeasurements().stream()
          .map(Sample::toString).collect(Collectors.joining("|")));
    });
  }
}
