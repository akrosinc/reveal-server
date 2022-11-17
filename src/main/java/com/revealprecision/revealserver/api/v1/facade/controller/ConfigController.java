package com.revealprecision.revealserver.api.v1.facade.controller;


import io.swagger.v3.oas.annotations.Operation;
import java.text.MessageFormat;
import lombok.RequiredArgsConstructor;
import org.keycloak.adapters.KeycloakDeployment;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/rest/config")
@RequiredArgsConstructor
public class ConfigController {

  private final KeycloakDeployment keycloakDeployment;

  @GetMapping("/keycloak")
  @Operation(summary = "Get Configured Keycloak details", description = "Get Configured Keycloak details", tags = {
      "Keycloak well-know configuration"})
  public ResponseEntity<String> getKeyCloakDetails() {
    String url = MessageFormat.format("{0}/realms/{1}/.well-known/openid-configuration",
        keycloakDeployment.getAuthServerBaseUrl(),
        keycloakDeployment.getRealm());
    return new RestTemplate().getForEntity(url, String.class);
  }
}
