package com.revealprecision.revealserver.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmihalcea.hibernate.type.util.ObjectMapperSupplier;

public class CustomObjectMapperSupplier implements ObjectMapperSupplier {

  @Override
  public ObjectMapper get() {
    ObjectMapper objectMapper = new ObjectMapper();

    objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    return objectMapper;
  }
}
