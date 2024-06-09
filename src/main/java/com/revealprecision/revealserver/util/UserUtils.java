package com.revealprecision.revealserver.util;

import java.security.Principal;
import org.apache.kafka.common.security.auth.KafkaPrincipal;
import org.keycloak.KeycloakPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserUtils {


  public static Principal getCurrentPrinciple() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof KeycloakPrincipal) {
      KeycloakPrincipal principal = (KeycloakPrincipal) authentication.getPrincipal();
      return principal;
    } else if (authentication != null && authentication.getPrincipal() instanceof KafkaPrincipal) {
      return (KafkaPrincipal) authentication.getPrincipal();
    } else {
      return null;
    }
  }

  public static boolean hasRole(String role){
    Principal currentPrinciple = getCurrentPrinciple();

    if (currentPrinciple instanceof KeycloakPrincipal){
      KeycloakPrincipal keycloakPrincipal = (KeycloakPrincipal) currentPrinciple;

      return keycloakPrincipal.getKeycloakSecurityContext().getToken().getRealmAccess().getRoles().contains(role);
    }

    return false;
  }

  public static String getCurrentPrincipleName() {
    return getCurrentPrinciple() == null ? null : getCurrentPrinciple().getName();
  }

  public static String getJwtKid() {
    String jwtKid = null;
    if (UserUtils.getCurrentPrinciple() instanceof KeycloakPrincipal) {
      if (UserUtils.getCurrentPrinciple() != null) {
        if (((KeycloakPrincipal) UserUtils.getCurrentPrinciple()).getKeycloakSecurityContext()
            != null) {
          if (((KeycloakPrincipal) UserUtils.getCurrentPrinciple()).getKeycloakSecurityContext()
              != null) {
            if (((KeycloakPrincipal) UserUtils.getCurrentPrinciple()).getKeycloakSecurityContext()
                .getToken() != null) {
              jwtKid = ((KeycloakPrincipal) UserUtils.getCurrentPrinciple()).getKeycloakSecurityContext()
                  .getToken().getId();
            }
          }
        }
      }
    } else {
      return null;
    }
    return jwtKid;
  }
}
