package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.enums.OrganizationTypeEnum;
import com.revealprecision.revealserver.persistence.domain.Organization;
import java.util.UUID;
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
public class GroupIdentifierNameTypeResponse {

  private UUID identifier;
  private String name;
  private OrganizationTypeEnum type;
}
