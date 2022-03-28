package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.NameUseEnum;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public  class PersonName {

  @NotNull
  private NameUseEnum use;
  @NotBlank
  private String text;
  @NotBlank
  private String family;
  private String given;
  private String prefix;
  private String suffix;
}
