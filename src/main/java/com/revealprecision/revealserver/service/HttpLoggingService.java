package com.revealprecision.revealserver.service;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.cloud.sleuth.Span;

public interface HttpLoggingService {

  void log(String path, Object requestObject, Object responseObject, Span span,
      String httpMethod, String httpCode, Map<String, String> headers, LocalDateTime requestTime,
      LocalDateTime responseTime, String requestor, String jwtKid);
}
