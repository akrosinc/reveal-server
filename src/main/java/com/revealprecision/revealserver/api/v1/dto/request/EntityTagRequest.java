package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.LookupEntityTypeCodeEnum;
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

  private String tag;

  private String valueType;

  private String definition;

  private LookupEntityTypeCodeEnum entityType;
}
