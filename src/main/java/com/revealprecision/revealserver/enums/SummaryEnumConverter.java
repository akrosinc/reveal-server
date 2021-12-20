package com.revealprecision.revealserver.enums;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class SummaryEnumConverter implements Converter<String, SummaryEnum> {

  @Override
  public SummaryEnum convert(String source) {
    return SummaryEnum.valueOf(source.toUpperCase());
  }
}
