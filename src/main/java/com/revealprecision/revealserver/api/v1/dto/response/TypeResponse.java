package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.enums.OrganizationTypeEnum;
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
public class TypeResponse {

  private OrganizationTypeEnum code;
  private String valueCodableConcept;
}
