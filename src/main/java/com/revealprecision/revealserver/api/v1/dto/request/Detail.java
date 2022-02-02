package com.revealprecision.revealserver.api.v1.dto.request;

import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Detail {

  @Valid
  private DetailQuantity detailQuantity;
}
