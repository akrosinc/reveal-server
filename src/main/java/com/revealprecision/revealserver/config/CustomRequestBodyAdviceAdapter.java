package com.revealprecision.revealserver.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.service.HttpLoggingService;
import com.revealprecision.revealserver.util.HeaderUtil;
import com.revealprecision.revealserver.util.UserUtils;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

@ControllerAdvice
@Slf4j
public class CustomRequestBodyAdviceAdapter extends RequestBodyAdviceAdapter {

  @Autowired
  HttpServletRequest httpServletRequest;

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  HttpLoggingService httpLoggingService;

  @Autowired
  private Tracer tracer;

  @Override
  public Object afterBodyRead(Object body, HttpInputMessage inputMessage,
      org.springframework.core.MethodParameter parameter, Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {

    Map<String, String> requestHeaders = HeaderUtil.getHeaderMapFromHttpServletRequest(
        httpServletRequest);

    LocalDateTime triggerTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(httpServletRequest.getSession().getCreationTime()),
        TimeZone.getDefault().toZoneId());

    String jwtKid = UserUtils.getJwtKid();

    httpLoggingService.log(
        httpServletRequest.getRequestURL().toString() + (httpServletRequest.getQueryString() != null
            ? "?" + httpServletRequest.getQueryString() : ""), body, null, tracer.currentSpan(),
        httpServletRequest.getMethod(), null, requestHeaders, triggerTime, null,
        UserUtils.getKeyCloakPrincipal().getName(), jwtKid);

    return super.afterBodyRead(body, inputMessage, parameter, targetType, converterType);
  }


  @Override
  public boolean supports(org.springframework.core.MethodParameter methodParameter, Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }
}
