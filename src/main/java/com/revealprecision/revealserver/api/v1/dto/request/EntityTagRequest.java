package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.LookupEntityTypeCodeEnum;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
public class EntityTagRequest {

  @NotBlank(message = "must not be blank")
  private String tag;

  @NotBlank(message = "must not be blank")
  private String valueType;

  private String definition;

  @NotNull
  private LookupEntityTypeCodeEnum entityType;
}
