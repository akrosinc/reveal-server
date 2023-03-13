package com.revealprecision.revealserver.config;


import com.revealprecision.revealserver.props.HttpLoggingProperties;
import com.revealprecision.revealserver.service.HttpLoggingService;
import com.revealprecision.revealserver.util.HeaderUtil;
import com.revealprecision.revealserver.util.UserUtils;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

//@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class CustomRequestBodyAdviceAdapter extends RequestBodyAdviceAdapter {

  private final HttpServletRequest httpServletRequest;

  private final HttpLoggingService httpLoggingService;

  private final HttpLoggingProperties httpLoggingProperties;

  private final Tracer tracer;

  public Object afterBodyRead(Object body, HttpInputMessage inputMessage,
      org.springframework.core.MethodParameter parameter, Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {

    Map<String, String> requestHeaders = HeaderUtil.getHeaderMapFromHttpServletRequest(
        httpServletRequest);

    LocalDateTime triggerTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(httpServletRequest.getSession().getCreationTime()),
        TimeZone.getDefault().toZoneId());

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
            httpServletRequest.getRequestURL().toString().contains(excludedPathString)
        ).findAny();

    if (excludedPath.isEmpty()) {
      httpLoggingService.log(
          httpServletRequest.getRequestURL().toString() + (
              httpServletRequest.getQueryString() != null
                  ? "?" + httpServletRequest.getQueryString() : ""), body, null,
          tracer.currentSpan(),
          httpServletRequest.getMethod(), null, requestHeaders, triggerTime, null,
          username, jwtKid);
    }

    return super.afterBodyRead(body, inputMessage, parameter, targetType, converterType);
  }


  @Override
  public boolean supports(org.springframework.core.MethodParameter methodParameter, Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }
}


