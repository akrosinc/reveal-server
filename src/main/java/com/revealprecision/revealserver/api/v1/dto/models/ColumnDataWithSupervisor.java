package com.revealprecision.revealserver.api.v1.dto.models;

import java.io.Serializable;
import java.util.List;
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
public class ColumnDataWithSupervisor implements Serializable {

  private UUID supervisorKey;
  private String supervisor;
  private ColumnData columnData;
}
