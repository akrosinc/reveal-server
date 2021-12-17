package com.revealprecision.revealserver.service;

import com.cosium.spring.data.jpa.entity.graph.domain.EntityGraphUtils;
import com.revealprecision.revealserver.api.v1.dto.factory.UserEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.UserRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.constant.Error;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.domain.User.Fields;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserService {

  private UserRepository userRepository;
  private KeycloakService keycloakService;
  private OrganizationService organizationService;

  @Autowired
  public UserService(UserRepository userRepository, KeycloakService keycloakService,
      OrganizationService organizationService) {
    this.userRepository = userRepository;
    this.keycloakService = keycloakService;
    this.organizationService = organizationService;
  }

  public User createUser(UserRequest userRequest) {
    if (userRequest.getEmail() == null) {
      if (userRepository.findByUserName(userRequest.getUserName()).isPresent()) {
        throw new ConflictException(
            String.format(Error.NON_UNIQUE, StringUtils.capitalize(Fields.userName),
                userRequest.getUserName()));
      }
    } else if (userRepository.findByUserNameOrEmail(userRequest.getUserName(),
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
    return userRepository.findByIdentifier(identifier)
        .orElseThrow(() -> new NotFoundException(Pair.of(
            Fields.identifier, identifier), User.class));
  }

  public Page<User> searchUsers(String searchParam, Pageable pageable) {
    return userRepository.searchByParameter(searchParam, pageable,
        EntityGraphUtils.fromAttributePaths(
            Fields.organizations));
  }

  public void deleteUser(UUID identifier) {
    User user = getByIdentifier(identifier);
    user.setEntityStatus(EntityStatus.DELETING);
    user = userRepository.save(user);
    keycloakService.deleteUser(user.getSid().toString(), user.getIdentifier());
    userRepository.delete(user);
  }

  public void updateUser(UUID identifier, UserRequest userRequest) {
    User user = getByIdentifier(identifier);
    keycloakService.updateUser(user.getSid().toString(), userRequest);
  }
}
