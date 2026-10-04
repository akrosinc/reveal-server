package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.dto.KeycloakRole;
import com.revealprecision.revealserver.enums.InstanceRoleEnum;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakRoleCatalog {

  private final Keycloak keycloak;

  @Value("${keycloak.realm}")
  private String realm;

  //  @Cacheable(value = "kc-role-list", key = "'org'")
  public List<KeycloakRole> listOrganizationRoles() {
    List<KeycloakRole> roles = listRolesByParentPath("/ORGANIZATION_ROLES");
    if (roles.isEmpty()) {
      roles = listRolesByParentPath("/organization-roles");
    }
    return roles;
  }

  //  @Cacheable(value = "kc-role-list", key = "'instance'")
  public List<KeycloakRole> listInstanceRoles() {
    List<KeycloakRole> roles = listRolesByParentPath("/INSTANCE_ROLES");
    if (roles.isEmpty()) {
      roles = listRolesByParentPath("/instance-roles");
    }
    return roles;
  }

  private List<KeycloakRole> listRolesByParentPath(String path) {
    GroupRepresentation parent;
    try {
      parent = keycloak.realm(realm).getGroupByPath(path);
    } catch (javax.ws.rs.NotFoundException e) {
      log.debug("Keycloak group path not found: {}", path);
      return Collections.emptyList();
    }
    // 401/403/other errors are NOT caught, so the real cause is visible

    List<GroupRepresentation> subGroups = keycloak.realm(realm).groups()
        .group(parent.getId()).toRepresentation()
        .getSubGroups();
    if (subGroups == null || subGroups.isEmpty()) {
      return Collections.emptyList();
    }

    return subGroups.stream()
        .map(g -> KeycloakRole.builder()
            .id(UUID.fromString(g.getId()))
            .name(g.getName())
            .permissions(getPermissionsForGroup(g.getId()))
            .build())
        .collect(Collectors.toList());
  }

  //  @Cacheable(value = "kc-role", key = "#groupId")
  public Optional<KeycloakRole> findById(UUID groupId) {
    if (groupId == null) {
      return Optional.empty();
    }
    try {
      GroupRepresentation group = keycloak.realm(realm).groups().group(groupId.toString())
          .toRepresentation();
      if (group == null) {
        return Optional.empty();
      }
      return Optional.of(toKeycloakRole(group));
    } catch (javax.ws.rs.NotFoundException e) {
      return Optional.empty();
    }
  }

  private KeycloakRole toKeycloakRole(GroupRepresentation g) {
    Set<String> permissions = g.getRealmRoles() == null
        ? Collections.emptySet()
        : new HashSet<>(g.getRealmRoles());
    return KeycloakRole.builder()
        .id(UUID.fromString(g.getId()))
        .name(g.getName())
        .permissions(permissions)
        .build();
  }

  public Optional<KeycloakRole> findById(String groupId) {
    if (groupId == null || groupId.trim().isEmpty()) {
      return Optional.empty();
    }
    try {
      return findById(UUID.fromString(groupId));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private Set<String> getPermissionsForGroup(String groupId) {
    try {
      return keycloak.realm(realm).groups().group(groupId).roles()
          .realmLevel()
          .listAll() // use listEffective() if composite roles are used
          .stream()
          .map(RoleRepresentation::getName)
          .collect(Collectors.toSet());
    } catch (javax.ws.rs.NotFoundException e) {
      return Collections.emptySet();
    }
  }

  public Optional<KeycloakRole> getInstanceRoleByName(String name) {
    if (name == null) {
      return Optional.empty();
    }
    return listInstanceRoles().stream()
        .filter(role -> role.getName().equalsIgnoreCase(name))
        .findFirst();
  }

  public KeycloakRole getInstanceAdminRole() {
    return getInstanceRoleByName(InstanceRoleEnum.ADMIN.name())
        .orElseThrow(() -> new NotFoundException("Instance role ADMIN not found in Keycloak"));
  }

  public KeycloakRole getInstanceStandardRole() {
    return getInstanceRoleByName(InstanceRoleEnum.STANDARD.name())
        .orElseThrow(() -> new NotFoundException("Instance role STANDARD not found in Keycloak"));
  }
}