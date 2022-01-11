package com.revealprecision.revealserver.enums;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class FlagEnumConverter implements Converter<String, FlagEnum> {

  @Override
  public FlagEnum convert(String source) {
    return FlagEnum.valueOf(source.toUpperCase());
  }
}
