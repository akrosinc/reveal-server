package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.UserRequest;
import com.revealprecision.revealserver.config.KeycloakConfig;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.KeycloakException;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KeycloakService {

  private static final Map<String, Boolean> access = Map.of(
      "manageGroupMembership", false,
      "view", false,
      "mapRoles", false,
      "impersonate", false,
      "manage", false
  );
  private final UserRepository userRepository;

  private static CredentialRepresentation createPasswordCredentials(String password,
      boolean temporary) {
    CredentialRepresentation passwordCredentials = new CredentialRepresentation();
    passwordCredentials.setTemporary(temporary);
    passwordCredentials.setType(CredentialRepresentation.PASSWORD);
    passwordCredentials.setValue(password);
    return passwordCredentials;
  }

  public String addUser(UserRequest userRequest, UUID identifier) {
    UsersResource usersResource = KeycloakConfig.getInstance().realm(KeycloakConfig.realm).users();
    CredentialRepresentation credentialRepresentation = createPasswordCredentials(
        userRequest.getPassword(), userRequest.isTempPassword());

    UserRepresentation kcUser = new UserRepresentation();
    kcUser.setUsername(userRequest.getUserName());
    kcUser.setCredentials(Collections.singletonList(credentialRepresentation));
    kcUser.setFirstName(userRequest.getFirstName());
    kcUser.setLastName(userRequest.getLastName());
    kcUser.setEmail(userRequest.getEmail());
    kcUser.setEnabled(true);
    kcUser.setTotp(false);
    kcUser.setEmailVerified(false);
    kcUser.setNotBefore(0);
    kcUser.setAccess(access);
    kcUser.setGroups(new ArrayList<>(userRequest.getSecurityGroups()));
    Response response = usersResource.create(kcUser);
    String output = response.readEntity(String.class);

    response.close();
    if (response.getStatus() == 201) {
      UserRepresentation kcCreatedUser = usersResource.search(userRequest.getUserName()).get(0);
      return kcCreatedUser.getId();
    } else if (response.getStatus() == 409) {
      JSONObject error = new JSONObject(output);
      userRepository.setApiResponse(identifier, error.getString("errorMessage"));
      throw new ConflictException(error.getString("errorMessage"));
    } else {
      userRepository.setApiResponse(identifier, "Unknown error on Keycloak");
      throw new KeycloakException("Unknown error on Keycloak");
    }
  }

  public void deleteUser(String kcId, UUID identifier) {
    UsersResource usersResource = KeycloakConfig.getInstance().realm(KeycloakConfig.realm).users();
    Response response = usersResource.delete(kcId);
    if (response.getStatus() == 404) {
      userRepository.setApiResponse(identifier, "User not found on Keycloak");
      throw new KeycloakException("User not found on Keycloak");
    } else if (response.getStatus() == 204) {
      return;
    } else {
      userRepository.setApiResponse(identifier, "Unknown error on Keycloak");
      throw new KeycloakException("Unknown error on Keycloak");
    }
  }

  public void updateUser(String kcId, UserRequest userRequest) {
    UserResource userResource = KeycloakConfig.getInstance().realm(KeycloakConfig.realm).users()
        .get(kcId);

    if (userResource != null) {
      UserRepresentation kcUser = userResource.toRepresentation();
      kcUser.setFirstName(userRequest.getFirstName());
      kcUser.setLastName(userRequest.getLastName());
      kcUser.setEmail(userRequest.getEmail());
      userResource.update(kcUser);
      updateGroups(kcId, userRequest.getSecurityGroups());
    } else {
      throw new KeycloakException("User not found in Keycloak");
    }
  }

  private void updateGroups(String kcId, Set<String> newGroups) {
    Map<String, GroupRepresentation> groups = KeycloakConfig.getInstance()
        .realm(KeycloakConfig.realm)
        .groups().groups().stream()
        .collect(Collectors.toMap(GroupRepresentation::getName, Function.identity()));
    UserResource userResource = KeycloakConfig.getInstance().realm(KeycloakConfig.realm).users()
        .get(kcId);

    groups.forEach(
        (s, groupRepresentation) -> userResource.leaveGroup(groupRepresentation.getId()));
    newGroups.stream().forEach(s -> {
      GroupRepresentation representation = groups.get(s);
      if (representation != null) {
        userResource.joinGroup(representation.getId());
      }
    });
  }
}
