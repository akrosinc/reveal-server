package com.revealprecision.revealserver.util;

import org.keycloak.KeycloakPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserUtils {

  public static KeycloakPrincipal getKeyCloakPrincipal() {
    KeycloakPrincipal principal = (KeycloakPrincipal) SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal();
    return principal;
  }
}
