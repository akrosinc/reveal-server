package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.response.RoleWithPermissionsResponse;
import com.revealprecision.revealserver.service.KeycloakRoleCatalog;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/role")
@RequiredArgsConstructor
public class RoleController {

  private final KeycloakRoleCatalog keycloakRoleCatalog;

  @Operation(summary = "Get organization roles", description = "Get all available organization roles with permissions", tags = {"Role"})
  @GetMapping(value = "/organization", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<RoleWithPermissionsResponse>> getOrganizationRoles() {
    List<RoleWithPermissionsResponse> roles = keycloakRoleCatalog.listOrganizationRoles().stream()
        .map(role -> RoleWithPermissionsResponse.builder()
            .identifier(role.getId())
            .name(role.getName())
            .permissions(role.getPermissions())
            .build())
        .collect(Collectors.toList());
    return ResponseEntity.ok(roles);
  }

  @Operation(summary = "Get instance roles", description = "Get all available instance roles with permissions", tags = {"Role"})
  @GetMapping(value = "/instance", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<RoleWithPermissionsResponse>> getInstanceRoles() {
    List<RoleWithPermissionsResponse> roles = keycloakRoleCatalog.listInstanceRoles().stream()
        .map(role -> RoleWithPermissionsResponse.builder()
            .identifier(role.getId())
            .name(role.getName())
            .permissions(role.getPermissions())
            .build())
        .collect(Collectors.toList());
    return ResponseEntity.ok(roles);
  }
}
