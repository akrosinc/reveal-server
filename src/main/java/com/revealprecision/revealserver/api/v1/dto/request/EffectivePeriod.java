package com.revealprecision.revealserver.api.v1.dto.request;

import java.time.LocalDate;
import javax.validation.constraints.FutureOrPresent;
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
public class EffectivePeriod {

  @FutureOrPresent
  private LocalDate start;

  @FutureOrPresent
  private LocalDate end;
}
