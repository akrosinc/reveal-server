package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.ReportTypeEnum;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TableReportRequest implements Serializable {

  private UUID planIdentifier;

  private ReportTypeEnum reportTypeEnum;

  @Nullable
  private UUID parentLocationIdentifier;

  private Boolean getChildren;
}
