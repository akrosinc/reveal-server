package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.UUID;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportDataRequest {
  @NotEmpty
  private String reportType;
  @NotNull
  private UUID planIdentifier;
  @NotNull
  private UUID parentIdentifier;
}
