package com.revealprecision.revealserver.api.v1.dto.models;

import java.io.Serializable;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RowDataWithSupervisorOrCdd implements Serializable {

  private String name;
  private String key;
  private String type;
  private Map<String, ColumnData> maps;

}
