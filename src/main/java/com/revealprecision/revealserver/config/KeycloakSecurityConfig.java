package com.revealprecision.revealserver.config;

import java.util.List;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(jsr250Enabled = true)
public class KeycloakSecurityConfig extends KeycloakWebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    super.configure(http);
    http.authorizeRequests()
        .anyRequest().permitAll();
    CorsConfiguration corsConfiguration = new CorsConfiguration();
    corsConfiguration.setAllowedHeaders(
        List.of("Authorization", "Cache-Control", "Content-Type"));
    corsConfiguration.setAllowedOriginPatterns(List.of("*"));
    corsConfiguration.setAllowedMethods(
        List.of("GET", "POST", "PUT", "DELETE", "PUT", "OPTIONS", "PATCH", "DELETE"));
    corsConfiguration.setAllowCredentials(true);
    corsConfiguration.setExposedHeaders(List.of("Authorization"));
    corsConfiguration.setMaxAge(3600L);
    http.cors().configurationSource(request -> corsConfiguration);
    http.csrf().disable();

    //TODO update configuration for production env
  }

  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    KeycloakAuthenticationProvider keycloakAuthenticationProvider = keycloakAuthenticationProvider();
    keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());
    auth.authenticationProvider(keycloakAuthenticationProvider);
  }

  @Bean
  @Override
  protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
    return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
  }

  @Bean
  public KeycloakConfigResolver KeycloakConfigResolver() {
    return new KeycloakSpringBootConfigResolver();
  }
}
