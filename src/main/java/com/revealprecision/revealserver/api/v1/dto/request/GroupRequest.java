package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.GroupTypeEnum;
import com.revealprecision.revealserver.persistence.domain.Location;
import java.util.UUID;
import lombok.Data;

@Data
public class GroupRequest {

  UUID identifier;
  String name;
  GroupTypeEnum type;
  UUID locationIdentifier;
  Location location;
}
