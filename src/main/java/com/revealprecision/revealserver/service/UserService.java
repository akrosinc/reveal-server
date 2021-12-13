package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.UserRequest;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private UserRepository userRepository;
  private KeycloakService keycloakService;

  @Autowired
  public UserService(UserRepository userRepository, KeycloakService keycloakService) {
    this.userRepository = userRepository;
    this.keycloakService = keycloakService;
  }

  public void createUser(UserRequest userRequest) {
    keycloakService.addUser(userRequest);

  }
}
