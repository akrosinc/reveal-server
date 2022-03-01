package com.revealprecision.revealserver.api.v1.facade.models;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationTag implements Serializable {

  private static final long serialVersionUID = -8367551045898354954L;

  private Long id;

  private Boolean active;

  private String name;

  private String description;

  private String locationId;

}
