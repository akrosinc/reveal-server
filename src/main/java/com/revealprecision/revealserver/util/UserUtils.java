package com.revealprecision.revealserver.util;

import org.keycloak.KeycloakPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserUtils {

  public static KeycloakPrincipal getKeyCloakPrincipal() {
    KeycloakPrincipal principal = (KeycloakPrincipal) SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal();
    return principal;
  }

  public static String getJwtKid() {
    String jwtKid = null;
    if (UserUtils.getKeyCloakPrincipal() != null) {
      if (UserUtils.getKeyCloakPrincipal().getKeycloakSecurityContext() != null) {
        if (UserUtils.getKeyCloakPrincipal().getKeycloakSecurityContext() != null) {
          if (UserUtils.getKeyCloakPrincipal().getKeycloakSecurityContext().getToken()
              != null) {
            jwtKid = UserUtils.getKeyCloakPrincipal().getKeycloakSecurityContext().getToken()
                .getId();
          }
        }
      }
    }
    return jwtKid;
  }
}
