package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.UserRequest;
import com.revealprecision.revealserver.config.KeycloakConfig;
import java.util.Collections;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

@Service
public class KeycloakService {

  private static final Map<String, Boolean> access = Map.of(
      "manageGroupMembership", false,
      "view", false,
      "mapRoles", false,
      "impersonate", false,
      "manage", false
  );

  private static CredentialRepresentation createPasswordCredentials(String password,
      boolean temporary) {
    CredentialRepresentation passwordCredentials = new CredentialRepresentation();
    passwordCredentials.setTemporary(temporary);
    passwordCredentials.setType(CredentialRepresentation.PASSWORD);
    passwordCredentials.setValue(password);
    return passwordCredentials;
  }

  public void addUser(UserRequest userRequest) {
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
    Response response = usersResource.create(kcUser);
  }
}
