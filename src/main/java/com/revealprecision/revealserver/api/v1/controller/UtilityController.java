package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/utility")
@ConditionalOnProperty(prefix = "controller.utility", name = "enable")
@CrossOrigin(originPatterns = "*", origins = "*")
public class UtilityController {

  private final UserService userService;


  @DeleteMapping("/user/deleteAll")
  public ResponseEntity<Void> deleteAll() { //only for testing purposes
    userService.deleteAll();
    return ResponseEntity.ok().build();
  }


  @DeleteMapping("/user/deleteAllInKeycloak")
  public ResponseEntity<Void> deleteAllInKeycloak() { //only for testing purposes
    userService.deleteAllInKeycloak();
    return ResponseEntity.ok().build();
  }

}
