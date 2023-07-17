package com.revealprecision.revealserver.api.v1.dto.models;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnData implements Serializable {

  private Object value;
  private Boolean isPercentage;
  private String meta;
  private String dataType = "double";
  private String key;
}
