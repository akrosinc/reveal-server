package com.revealprecision.revealserver.service.models;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class TaskSearchCriteria {

  private UUID planIdentifier;

  private UUID taskStatusIdentifier;

  private UUID actionIdentifier;

  private UUID locationIdentifier;

}
