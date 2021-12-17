package com.revealprecision.revealserver.api.v1.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationCriteria {


  private String search = "";
  private boolean root;
}
