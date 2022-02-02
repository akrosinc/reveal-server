package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.enums.UnitEnum;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetResponse {

  private UUID identifier;
  private String measure;
  private int value;
  private String comparator;
  private UnitEnum unit;
  private LocalDate due;
}
