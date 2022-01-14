package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.UnitEnum;
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
public class DetailQuantity {

  @NotNull
  private Integer value;

  @NotBlank
  private String comparator;

  @NotNull
  private UnitEnum unit;
}
