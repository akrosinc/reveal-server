package com.revealprecision.revealserver.api.v1.facade.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateLocationRequest extends PhysicalLocation {

  private String syncStatus;

  private Long rowid;

}
