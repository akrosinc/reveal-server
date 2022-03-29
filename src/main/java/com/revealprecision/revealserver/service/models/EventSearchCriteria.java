package com.revealprecision.revealserver.service.models;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class EventSearchCriteria {

  private List<String> organizationName;

  private List<UUID> organizationIdentifier;

  private List<UUID> providerIdentifier;

  private List<UUID> locationIdentifier;

  private List<UUID> baseIdentifier;

}
