package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.OrganizationTypeEnum;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationCriteria {

  private String name;
  private UUID identifier;
  private UUID partOf;
  private OrganizationTypeEnum type;
}
