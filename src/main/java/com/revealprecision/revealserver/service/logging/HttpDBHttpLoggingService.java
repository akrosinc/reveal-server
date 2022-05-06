package com.revealprecision.revealserver.service.logging;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.persistence.domain.logging.HttpLogging;
import com.revealprecision.revealserver.persistence.repository.logging.HttpLoggingRepository;
import com.revealprecision.revealserver.props.HttpLoggingProperties;
import com.revealprecision.revealserver.service.HttpLoggingService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Span;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Profile("Http-DB-Logging")
@Service
@RequiredArgsConstructor
@Slf4j
public class HttpDBHttpLoggingService implements HttpLoggingService {

  private final HttpLoggingRepository httpLoggingRepository;
  private final ObjectMapper objectMapper;
  private final HttpLoggingProperties httpLoggingProperties;

  @Async
  @Override
  public void log(String path, Object requestObject, Object responseObject, Span span,
      String httpMethod, String httpCode, Map<String, String> headers, LocalDateTime requestTime,
      LocalDateTime responseTime, String requestor, String jwtKid) {
    try {
      JsonNode request = requestObject != null ? objectMapper.readTree(
          objectMapper.writeValueAsString(requestObject)) : null;

      JsonNode response = null;
      try {
        response = responseObject != null ? objectMapper.readTree(
            objectMapper.writeValueAsString(responseObject)) : null;
      } catch (JsonProcessingException e){
        log.warn("Response is not a JSON object {}",requestObject);
      }
      String traceId = span != null ? span.context().traceId() : null;

      String spanId = span != null ? span.context().spanId() : null;

      JsonNode httpHeaders =
          headers != null ? objectMapper.readTree(objectMapper.writeValueAsString(headers)) : null;

      logMessage(HttpLogging.builder().request(request).response(response).requestTime(requestTime)
          .responseTime(responseTime).path(path).identifier(UUID.randomUUID()).traceId(traceId)
          .spanId(spanId).httpMethod(httpMethod).httpCode(httpCode).httpHeaders(httpHeaders)
          .requestor(requestor).jwtKid(jwtKid).build());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }


  public void logMessage(HttpLogging httpLogging) {
    if (httpLoggingProperties.isShouldLogToConsole()) {
      log.debug("{}", pretty(httpLogging));
    }
    if (httpLoggingProperties.isShouldLogToDatabase()) {
      httpLoggingRepository.save(httpLogging);
    }
  }

  @Transactional
  public void cleanUpLog() {
    Duration duration = Duration.of(httpLoggingProperties.getChronoAmount(),
        httpLoggingProperties.getChronoUnit());
    httpLoggingRepository.deleteAllByRequestTimeBefore(LocalDateTime.now().minus(duration));
  }


  private String pretty(HttpLogging httpLogging) {
    return "httpMethod='" + httpLogging.getHttpMethod() + '\'' + ", path='" + httpLogging.getPath()
        + '\'' + ", httpCode='" + httpLogging.getHttpCode() + '\'' + ", request=" + (
        httpLogging.getRequest() != null ? httpLogging.getRequest().toString() != null ?
            httpLogging.getRequest().toString().length() > httpLoggingProperties.getLogLength()
                ? httpLogging.getRequest().toString()
                .substring(0, httpLoggingProperties.getLogLength()).concat("...")
                : httpLogging.getRequest().toString() : null : null) + ", response=" + (
        httpLogging.getResponse() != null ? httpLogging.getResponse().toString() != null ?
            httpLogging.getResponse().toString().length() > httpLoggingProperties.getLogLength()
                ? httpLogging.getResponse().toString()
                .substring(0, httpLoggingProperties.getLogLength()).concat("...")
                : httpLogging.getResponse().toString() : null : null);
  }
}
