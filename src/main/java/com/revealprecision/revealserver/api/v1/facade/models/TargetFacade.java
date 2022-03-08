package com.revealprecision.revealserver.api.v1.facade.models;

import com.revealprecision.revealserver.api.v1.dto.request.Detail;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TargetFacade {

  private String measure;
  private Detail detail;
  private LocalDate due;
}