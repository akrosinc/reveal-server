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

  private List<String> organizationNames;

  private List<UUID> organizationIdentifiers;

  private List<UUID> userIdentifiers;

  private List<UUID> locationIdentifiers;

  private List<UUID> baseIdentifiers;

  private List<String> userNames;

  private Long serverVersion;

}
