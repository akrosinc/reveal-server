package com.revealprecision.revealserver.config;

import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

public class KeycloakConfig {

  public final static String serverUrl = "https://sso-ops.akros.online/auth";
  public final static String realm = "reveal";
  public final static String clientId = "reveal-server";
  public final static String clientSecret = "380cd0be-e0bb-4867-b5c6-24776d05cc1a";
  public final static String userName = "reveal.admin";
  public final static String password = "unsecure";
  public static Keycloak keycloak = null;

  public KeycloakConfig() {
  }

  public static Keycloak getInstance() {
    if (keycloak == null) {

      keycloak = KeycloakBuilder.builder()
          .serverUrl(serverUrl)
          .realm(realm)
          .grantType(OAuth2Constants.PASSWORD)
          .username(userName)
          .password(password)
          .clientId(clientId)
          .clientSecret(clientSecret)
          .resteasyClient(new ResteasyClientBuilderImpl().connectionPoolSize(20).build())
          .build();
    }
    return keycloak;
  }
}