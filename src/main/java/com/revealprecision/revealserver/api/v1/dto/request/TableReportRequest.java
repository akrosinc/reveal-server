package com.revealprecision.revealserver.api.v1.dto.request;

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

  private UUID reportIdentifier;

  @Nullable
  private UUID parentLocationIdentifier;

  private Boolean getChildren;
}
