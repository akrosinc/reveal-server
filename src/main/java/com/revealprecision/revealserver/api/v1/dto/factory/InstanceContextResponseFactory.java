package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.IdentifierNameResponse;
import com.revealprecision.revealserver.api.v1.dto.response.InstanceContextResponse;
import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.InstanceRole;
import com.revealprecision.revealserver.persistence.domain.InstanceUser;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.OrganizationRole;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstanceContextResponseFactory {

  public static InstanceContextResponse buildInstanceContextResponse(
      InstanceUser instanceUser,
      List<InstanceContextResponse.GroupContextInfo> groups) {

    Instance instance = instanceUser.getInstance();
    InstanceRole role = instanceUser.getRole();

    Set<String> instancePermissions = role.getPermissions().stream()
        .map(p -> p.getPermission().getName())
        .collect(Collectors.toSet());

    InstanceContextResponse.InstanceRoleInfo roleInfo = InstanceContextResponse.InstanceRoleInfo.builder()
        .identifier(role.getIdentifier())
        .name(role.getName())
        .permissions(instancePermissions)
        .build();

    IdentifierNameResponse selectedInstance = IdentifierNameResponse.builder()
        .identifier(instance.getIdentifier())
        .name(instance.getName())
        .build();

    return InstanceContextResponse.builder()
        .selectedInstance(selectedInstance)
        .role(roleInfo)
        .groups(groups)
        .build();
  }

  public static InstanceContextResponse.GroupContextInfo toGroupContextInfo(
      Organization org, List<OrganizationRole> orgRoles) {

    List<InstanceContextResponse.GroupRoleInfo> groupRoleInfos = orgRoles.stream()
        .map(InstanceContextResponseFactory::toGroupRoleInfo)
        .collect(Collectors.toList());

    Set<String> groupPermissions = orgRoles.stream()
        .flatMap(role -> role.getPermissions().stream())
        .map(p -> p.getPermission().getName())
        .collect(Collectors.toSet());

    return InstanceContextResponse.GroupContextInfo.builder()
        .identifier(org.getIdentifier())
        .name(org.getName())
        .type(org.getType().name())
        .roles(groupRoleInfos)
        .permissions(groupPermissions)
        .build();
  }

  private static InstanceContextResponse.GroupRoleInfo toGroupRoleInfo(OrganizationRole role) {
    return InstanceContextResponse.GroupRoleInfo.builder()
        .identifier(role.getIdentifier())
        .name(role.getName())
        .permissions(role.getPermissions().stream()
            .map(p -> p.getPermission().getName())
            .collect(Collectors.toSet()))
        .build();
  }
}