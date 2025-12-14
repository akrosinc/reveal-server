package com.revealprecision.revealserver.amdr.model;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KeyValue implements Serializable {

  private String key;
  private String number;
}
