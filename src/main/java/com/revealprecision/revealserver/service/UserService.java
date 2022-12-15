package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.UserEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.UserPasswordRequest;
import com.revealprecision.revealserver.api.v1.dto.request.UserRequest;
import com.revealprecision.revealserver.api.v1.dto.request.UserUpdateRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.constant.Error;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.domain.User.Fields;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import com.revealprecision.revealserver.util.UserUtils;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final KeycloakService keycloakService;
  private final OrganizationService organizationService;
  private final Keycloak keycloak;
  @Value("${keycloak.realm}")
  private String realm;

  public User createUser(UserRequest userRequest) {
    if (userRequest.getEmail() == null) {
      if (userRepository.getByUsername(userRequest.getUsername()).isPresent()) {
        throw new ConflictException(
            String.format(Error.NON_UNIQUE, StringUtils.capitalize(Fields.username),
                userRequest.getUsername()));
      }
    } else if (userRepository.findByUserNameOrEmail(userRequest.getUsername(),
        userRequest.getEmail()).isPresent()) {
      throw new ConflictException(
          "Username and email must be unique!"); //TODO This could be refactored to be prettier
    }

    Set<Organization> organizations = organizationService.findByIdentifiers(
        userRequest.getOrganizations());
    User user = UserEntityFactory.toEntity(userRequest, organizations);
    user.setEntityStatus(EntityStatus.CREATING);
    user = userRepository.save(user);

    UUID keyCloakId = UUID.fromString(keycloakService.addUser(userRequest, user.getIdentifier()));
    user.setSid(keyCloakId);
    user.setEntityStatus(EntityStatus.ACTIVE);
    return userRepository.save(user);
  }

  public User getByIdentifier(UUID identifier) {
    return userRepository.findByIdentifier(identifier).orElseThrow(
        () -> new NotFoundException(Pair.of(Fields.identifier, identifier), User.class));
  }

  public Page<User> searchUsers(String searchParam, Pageable pageable) {
    return userRepository.searchByParameter(searchParam, pageable);
  }

  public long getUsersNumber() {
    return userRepository.getNumberOfUsers();
  }

  public void deleteUser(UUID identifier) {
    User user = getByIdentifier(identifier);
    user.setEntityStatus(EntityStatus.DELETING);
    user = userRepository.save(user);
    keycloakService.deleteUser(user.getSid().toString(), user.getIdentifier());
    userRepository.delete(user);
  }

  public void updateUser(UUID identifier, UserUpdateRequest userRequest) {
    User user = getByIdentifier(identifier);
    if (userRequest.getEmail() != null) {
      userRepository.findByEmail(userRequest.getEmail()).ifPresent(user1 -> {
        if (!user1.getIdentifier().equals(user.getIdentifier())) {
          throw new ConflictException(
              String.format(Error.NON_UNIQUE, StringUtils.capitalize(Fields.email),
                  userRequest.getEmail()));
        }
      });
    }
    Set<String> securityGroups = keycloakService.updateUser(user.getSid().toString(), userRequest);
    user.updateUser(userRequest);
    user.setOrganizations(organizationService.findByIdentifiers(userRequest.getOrganizations()));
    user.setSecurityGroups(securityGroups);
    userRepository.save(user);
    //TODO Updating user could be upgraded if nedeed
  }

  public void resetPassword(UUID identifier, UserPasswordRequest passwordRequest) {
    User user = getByIdentifier(identifier);
    keycloakService.resetPassword(user.getSid().toString(), passwordRequest);
  }

  public User getByKeycloakId(UUID id) {
    return userRepository.findBySid(id)
        .orElseThrow(() -> new NotFoundException(Pair.of(Fields.sid, id), User.class));
  }

  public void deleteAll() {
    List<User> users = userRepository.findAll();
    UsersResource userResource = keycloak.realm(realm).users();
    keycloakService.deleteAll(users, userResource);
  }

  public User getCurrentUser() {
    return getByKeycloakId(UUID.fromString(UserUtils.getCurrentPrinciple().getName()));
  }

  public User getByUserName(String username) {
    return userRepository.getByUsername(username)
        .orElseThrow(() -> new NotFoundException(Pair.of(Fields.username, username), User.class));
  }

  public User findByUsername(String username) {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new NotFoundException(Pair.of(Fields.username, username), User.class));
  }

}
