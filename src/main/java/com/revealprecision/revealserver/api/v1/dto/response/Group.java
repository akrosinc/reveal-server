package com.revealprecision.revealserver.api.v1.dto.response;

import java.io.Serializable;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Group implements Serializable {

  private UUID identifier;
  private String name;
  private String type;

}
