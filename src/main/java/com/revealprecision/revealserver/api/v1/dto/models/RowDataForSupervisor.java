package com.revealprecision.revealserver.api.v1.dto.models;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RowDataForSupervisor extends  RowData implements Serializable {

  private List<RowDataWithSupervisorOrCdd> rowDataWithSupervisorOrCdds;

}
