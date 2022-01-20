package com.revealprecision.revealserver.api.v1.dto.request;

import java.time.LocalDate;
import javax.validation.Valid;
import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TargetRequest {

  @NotBlank
  private String measure;

  @Valid
  private Detail detail;

  @FutureOrPresent
  @NotNull
  private LocalDate due;
}
