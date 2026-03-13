package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.IdentifierNameResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceContextResponse;
import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.InstanceRole;
import com.revealprecision.revealserver.persistence.domain.InstanceUser;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstanceContextResponseFactory {

  public static InstanceContextResponse buildInstanceContextResponse(InstanceUser instanceUser) {
    Instance instance = instanceUser.getInstance();
    InstanceRole role = instanceUser.getRole();

    Set<String> permissions = role.getPermissions().stream()
        .map(instanceRolePermission ->
            instanceRolePermission.getPermission().getName())
        .collect(Collectors.toSet());

    IdentifierNameResponse identifierNameResponse = IdentifierNameResponse.builder()
        .identifier(instance.getIdentifier())
        .name(instance.getName())
        .build();

    return InstanceContextResponse.builder()
        .selectedInstance(identifierNameResponse)
        .roleIdentifier(role.getIdentifier())
        .roleName(role.getName())
        .permissions(permissions)
        .build();
  }
}
