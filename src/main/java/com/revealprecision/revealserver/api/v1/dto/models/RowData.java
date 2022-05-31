package com.revealprecision.revealserver.api.v1.dto.models;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;
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
public class RowData implements Serializable {

  private UUID locationIdentifier;
  private String locationName;
  private Long childrenNumber;
  private Map<String, ColumnData> columnDataMap;
}
