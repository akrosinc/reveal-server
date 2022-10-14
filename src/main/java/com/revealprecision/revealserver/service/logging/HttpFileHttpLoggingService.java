package com.revealprecision.revealserver.service.logging;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.persistence.domain.logging.HttpLoggingModel;
import com.revealprecision.revealserver.props.HttpLoggingProperties;
import com.revealprecision.revealserver.service.HttpLoggingService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.sleuth.Span;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Profile("Http-File-Logging")
@Service
@RequiredArgsConstructor
@Slf4j
public class HttpFileHttpLoggingService implements HttpLoggingService {

  private final ObjectMapper objectMapper;
  private final HttpLoggingProperties httpLoggingProperties;
  private final Logger altLog = LoggerFactory.getLogger("http-file");

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
      } catch (JsonProcessingException e) {
        log.warn("Response is not a JSON object {}", requestObject);
      }
      String traceId = span != null ? span.context().traceId() : null;

      String spanId = span != null ? span.context().spanId() : null;

      JsonNode httpHeaders =
          headers != null ? objectMapper.readTree(objectMapper.writeValueAsString(headers)) : null;

      logMessage(
          HttpLoggingModel.builder().request(request).response(response).requestTime(requestTime)
              .responseTime(responseTime).path(path).identifier(UUID.randomUUID()).traceId(traceId)
              .spanId(spanId).httpMethod(httpMethod).httpCode(httpCode).httpHeaders(httpHeaders)
              .requestor(requestor).jwtKid(jwtKid).build());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      log.error("Json Exception converting json to string", e);
    }
  }


  public void logMessage(HttpLoggingModel httpLogging) {
    if (httpLoggingProperties.isShouldLogToConsole()) {
      log.debug("{}", pretty(httpLogging));
      MDC.put("correlation", UUID.randomUUID().toString());
      MDC.put("http_code", httpLogging.getHttpCode());
      MDC.put("http_method", httpLogging.getHttpMethod());
      MDC.put("jwt_kid", httpLogging.getJwtKid());
      MDC.put("requestor", httpLogging.getRequestor());
      MDC.put("path", httpLogging.getPath());
      MDC.put("request_time",
          httpLogging.getRequestTime() != null ? httpLogging.getRequestTime().toString() : null);
      MDC.put("response_time",
          httpLogging.getResponseTime() != null ? httpLogging.getResponseTime().toString() : null);
      altLog.debug("request: {} ", httpLogging.getRequest());

      if (httpLoggingProperties.isShouldHideLocationResponses()) {
        altLog.debug("response: {}", httpLogging.getResponse() != null ?
            httpLogging.getResponse().toString().length() > 500 ?
                httpLogging.getResponse().toString().substring(0, 500).contains("MultiPolygon") &&
                    httpLogging.getResponse().toString().substring(0, 500).contains("coordinates") ?
                    httpLogging.getResponse().toString().substring(0, 500)
                    : httpLogging.getResponse()
                : httpLogging.getResponse() : null);
      } else {
        altLog.debug("response: {}", httpLogging.getResponse());
      }
      MDC.clear();
    }
  }


  private String pretty(HttpLoggingModel httpLogging) {
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
