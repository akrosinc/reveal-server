package com.revealprecision.revealserver.messaging.message;

import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
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

  private String supervisor;
  private String supervisorKey;
  private String cdd;
  private String cddKey;
  private String type;
  private Map<String, ColumnData> maps;

}