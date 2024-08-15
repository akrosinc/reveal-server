package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.UserPasswordRequest;
import com.revealprecision.revealserver.api.v1.dto.request.UserRequest;
import com.revealprecision.revealserver.api.v1.dto.request.UserUpdateRequest;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.KeycloakException;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {

  public static final Map<String, Boolean> access = Map.of(
      "manageGroupMembership", false,
      "view", false,
      "mapRoles", false,
      "impersonate", false,
      "manage", false
  );
  private final UserRepository userRepository;
  private final Keycloak keycloak;
  @Value("${keycloak.realm}")
  private String realm;

  @Value("{keycloak.auth-server-url}")
  private String serverUrl;

  public static CredentialRepresentation createPasswordCredentials(String password,
      boolean temporary) {
    CredentialRepresentation passwordCredentials = new CredentialRepresentation();
    passwordCredentials.setTemporary(temporary);
    passwordCredentials.setType(CredentialRepresentation.PASSWORD);
    passwordCredentials.setValue(password);
    return passwordCredentials;
  }

  public String addUser(UserRequest userRequest, UUID identifier) {
    UsersResource usersResource = keycloak.realm(realm).users();
    CredentialRepresentation credentialRepresentation = createPasswordCredentials(
        userRequest.getPassword(), userRequest.isTempPassword());

    UserRepresentation kcUser = new UserRepresentation();
    kcUser.setUsername(userRequest.getUsername());
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
      UserRepresentation kcCreatedUser = usersResource.search(userRequest.getUsername()).get(0);
      return kcCreatedUser.getId();
    } else if (response.getStatus() == 409) {
      JSONObject error = new JSONObject(output);
      List<UserRepresentation> userList = usersResource.search(userRequest.getUsername());
      if (!userList.isEmpty()) {
        UserRepresentation user = userList.get(0);
        updateUser(user.getId(), UserUpdateRequest.builder().email(userRequest.getEmail())
            .firstName(userRequest.getFirstName()).lastName(userRequest.getLastName())
            .email(userRequest.getEmail()).organizations(userRequest.getOrganizations())
            .securityGroups(userRequest.getSecurityGroups()).build());
        return user.getId();
      } else {
        userRepository.setApiResponse(identifier, error.getString("errorMessage"));
        throw new ConflictException(error.getString("errorMessage"));
      }
    } else {
      userRepository.setApiResponse(identifier, "Unknown error on Keycloak");
      throw new KeycloakException("Unknown error on Keycloak");
    }
  }

  public void deleteUser(String kcId, UUID identifier) {
    UsersResource usersResource = keycloak.realm(realm).users();
    Response response = usersResource.delete(kcId);
    if (response.getStatus() == 404) {
      userRepository.setApiResponse(identifier, "User not found on Keycloak");
      throw new KeycloakException("User not found on Keycloak");
    } else if (response.getStatus() == 204) {
      log.info("keycloak response: {}",response.getStatus());
    } else {
      userRepository.setApiResponse(identifier, "Unknown error on Keycloak");
      throw new KeycloakException("Unknown error on Keycloak");
    }
  }
  public RoleMappingResource getUserRoles(String kcId){
    UserResource userResource = keycloak.realm(realm).users()
        .get(kcId);
    return userResource.roles();
  }

//  public void generateTokenForUser(String token){
////    Keycloak keycloak = Keycloak.getInstance(serverUrl,realm,"reveal-web","Bearer ".concat(token));
//    TokenManager tokenManager = keycloak.tokenManager();
//    AccessTokenResponse accessToken = tokenManager.getAccessToken();
//    keycloak
//    System.out.println(accessToken);
//  }

  public Set<String> updateUser(String kcId, UserUpdateRequest userRequest) {
    UserResource userResource = keycloak.realm(realm).users()
        .get(kcId);

    if (userResource != null) {
      UserRepresentation kcUser = userResource.toRepresentation();
      kcUser.setFirstName(userRequest.getFirstName());
      kcUser.setLastName(userRequest.getLastName());
      kcUser.setEmail(userRequest.getEmail() == null ? "" : userRequest.getEmail());
      userResource.update(kcUser);
      return updateGroups(kcId, userRequest.getSecurityGroups());
    } else {
      throw new KeycloakException("User not found in Keycloak");
    }
  }


  private Set<String> updateGroups(String kcId, Set<String> newGroups) {
    Set<String> response = new HashSet<>();
    Map<String, GroupRepresentation> groups = keycloak
        .realm(realm)
        .groups().groups().stream()
        .collect(Collectors.toMap(GroupRepresentation::getName, Function.identity()));
    UserResource userResource = keycloak.realm(realm).users()
        .get(kcId);

    groups.forEach(
        (s, groupRepresentation) -> userResource.leaveGroup(groupRepresentation.getId()));
    newGroups.stream()
        .filter(groups::containsKey)
        .forEach(s -> {
          GroupRepresentation representation = groups.get(s);
          userResource.joinGroup(representation.getId());
          response.add(s);
        });
    return response;
  }

  public void resetPassword(String kcId, UserPasswordRequest request) {
    UserResource userResource = keycloak.realm(realm).users()
        .get(kcId);

    CredentialRepresentation credentialRepresentation = createPasswordCredentials(
        request.getPassword(), request.isTempPassword());

    userResource.resetPassword(credentialRepresentation);
  }

  @Async
  public void deleteAll(List<User> users, UsersResource resource) {
    int i = 0;
    for (User u : users) {
      if (u.getSid() != null) {
        resource.delete(u.getSid().toString());
        System.out.println(++i);
      }
    }
  }

  @Async
  public void deleteAllInKeycloak(List<UserRepresentation> users, UsersResource resource) {
    int i = 0;
    for (UserRepresentation u : users) {
      if (u.getId() != null) {
        resource.delete(u.getId());
        System.out.println(++i);
      }
    }
  }

}
