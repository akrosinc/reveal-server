package com.revealprecision.revealserver.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
@Builder
public class ParentMap {

  private String locationId;

  private String locationName;

  private String parentId;

  private String parentName;

  private int nodeLevel;
}
