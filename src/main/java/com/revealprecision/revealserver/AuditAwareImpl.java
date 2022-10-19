package com.revealprecision.revealserver;

import com.revealprecision.revealserver.util.UserUtils;
import java.util.Optional;
import org.apache.kafka.common.security.auth.KafkaPrincipal;
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
      KeycloakPrincipal principal = (KeycloakPrincipal) UserUtils.getCurrentPrinciple();
      return Optional.of(principal.getName());
    } else if (authentication != null && authentication.getPrincipal() instanceof KafkaPrincipal){
      KafkaPrincipal principal = (KafkaPrincipal) authentication.getPrincipal();
      return Optional.of(principal.getName());
    }
    return Optional.of("default");
  }
}