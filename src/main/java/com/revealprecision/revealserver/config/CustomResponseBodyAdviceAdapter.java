package com.revealprecision.revealserver.config;

import com.revealprecision.revealserver.props.HttpLoggingProperties;
import com.revealprecision.revealserver.service.HttpLoggingService;
import com.revealprecision.revealserver.util.HeaderUtil;
import com.revealprecision.revealserver.util.UserUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;


@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class CustomResponseBodyAdviceAdapter implements ResponseBodyAdvice<Object> {


  private final HttpLoggingService httpLoggingService;

  private final Tracer tracer;

  private final HttpLoggingProperties httpLoggingProperties;

  @Override
  public boolean supports(MethodParameter methodParameter,
      Class<? extends HttpMessageConverter<?>> aClass) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(Object o, MethodParameter methodParameter, MediaType mediaType,
      Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest,
      ServerHttpResponse serverHttpResponse) {

    if (serverHttpRequest instanceof ServletServerHttpRequest
        && serverHttpResponse instanceof ServletServerHttpResponse) {

      HttpServletRequest servletRequest = ((ServletServerHttpRequest) serverHttpRequest).getServletRequest();

      Map<String, String> headers = HeaderUtil.getHeaderMapFromServerHttpRequest(serverHttpRequest);

      servletRequest.getSession();

      LocalDateTime triggerTime = LocalDateTime.ofInstant(
          Instant.ofEpochMilli(servletRequest.getSession().getCreationTime()),
          TimeZone.getDefault().toZoneId());
      try {
        String jwtKid = null;
        try {
          jwtKid = UserUtils.getJwtKid();
        } catch (ClassCastException | NullPointerException e) {
          log.warn("No keycloak principal available");
          jwtKid = "not available";
        }
        String username = null;
        try {
          username = UserUtils.getCurrentPrincipleName();
        } catch (ClassCastException | NullPointerException e) {
          log.warn("No keycloak username available");
          username = "not available";
        }

        Optional<String> excludedPath = httpLoggingProperties.getExcludedPaths().stream()
            .filter(excludedPathString ->
                servletRequest.getRequestURL().toString().contains(excludedPathString)
            ).findAny();

        if (excludedPath.isEmpty()) {
          httpLoggingService.log(
              servletRequest.getRequestURL().toString() + (servletRequest.getQueryString() != null ?
                  "?"
                      + servletRequest.getQueryString() : ""), null, o, tracer.currentSpan(),
              servletRequest.getMethod(), String.valueOf(
                  ((ServletServerHttpResponse) serverHttpResponse).getServletResponse()
                      .getStatus()),
              headers, triggerTime, LocalDateTime.now(), username,
              jwtKid);
        }
      } catch (ClassCastException e) {
        log.error("Failure to log http response ",e);
      }
    }
    return o;
  }


}
