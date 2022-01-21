package com.revealprecision.revealserver;

import com.revealprecision.revealserver.util.UserUtils;
import java.util.Optional;
import org.keycloak.KeycloakPrincipal;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuditAwareImpl implements AuditorAware<String> {

  @Override
  public Optional<String> getCurrentAuditor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof KeycloakPrincipal) {
      KeycloakPrincipal principal = UserUtils.getKeyCloakPrincipal();
      return Optional.of(principal.getName());
    }
    return Optional.empty();
  }
}