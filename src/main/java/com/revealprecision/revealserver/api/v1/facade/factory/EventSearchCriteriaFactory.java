package com.revealprecision.revealserver.api.v1.facade.factory;

import com.revealprecision.revealserver.api.v1.facade.models.SyncParamFacade;
import com.revealprecision.revealserver.service.models.EventSearchCriteria;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public class EventSearchCriteriaFactory {

  public static EventSearchCriteria getEventSearchCriteria(SyncParamFacade syncParam) {
    return EventSearchCriteria.builder()
        .baseIdentifiers(syncParam.getBaseEntityId() == null ? null
            : Arrays.stream(syncParam.getBaseEntityId().split(",")).map(UUID::fromString)
                .collect(Collectors.toList()))
        .locationIdentifiers(
            syncParam.getLocationId() == null ? null
                : Arrays.stream(syncParam.getLocationId().split(",")).map(UUID::fromString)
                    .collect(Collectors.toList()))
        .organizationIdentifiers(
            syncParam.getTeamId() == null ? null
                : Arrays.stream(syncParam.getTeamId().split(",")).map(UUID::fromString)
                    .collect(Collectors.toList()))
        .organizationNames(syncParam.getTeam() == null ? null
            : Arrays.stream(syncParam.getTeam().split(",")).collect(Collectors.toList()))
        .userNames(
            syncParam.getProviderId() == null ? null
                : Arrays.stream(syncParam.getProviderId().split(",")).collect(Collectors.toList()))
        .build();
  }
}
