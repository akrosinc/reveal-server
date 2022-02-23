package com.revealprecision.revealserver.api.v1.facade.controller;


import lombok.RequiredArgsConstructor;
import org.keycloak.adapters.KeycloakDeployment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/config")
@RequiredArgsConstructor
public class ConfigController {

  private final KeycloakDeployment keycloakDeployment;

  @GetMapping("/keycloak")
  public ResponseEntity<String> getKeyCloakDetails() {
    String tokenURL = keycloakDeployment.getTokenUrl();
    return ResponseEntity.ok(tokenURL);
  }
}
