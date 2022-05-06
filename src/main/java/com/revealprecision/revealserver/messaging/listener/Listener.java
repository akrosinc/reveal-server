package com.revealprecision.revealserver.messaging.listener;

import java.security.Principal;
import org.apache.kafka.common.security.auth.KafkaPrincipal;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class Listener {

  //TODO: this is a hack and a proper solution is required for this to enable the correct user to represent the modifiedby and createdby details for envers
  public void init() {
    Principal principal = new KafkaPrincipal("internal", "kafka");
    Authentication authentication = new AbstractAuthenticationToken(null) {
      @Override
      public Object getCredentials() {
        return null;
      }

      @Override
      public Object getPrincipal() {
        return principal;
      }
    };

    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
