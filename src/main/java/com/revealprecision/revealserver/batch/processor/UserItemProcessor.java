package com.revealprecision.revealserver.batch.processor;

import com.revealprecision.revealserver.api.v1.dto.factory.UserEntityFactory;
import com.revealprecision.revealserver.batch.BatchConstants;
import com.revealprecision.revealserver.batch.BatchConstants.Constraint;
import com.revealprecision.revealserver.batch.dto.UserBatchDTO;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.domain.UserBulk;
import com.revealprecision.revealserver.persistence.domain.UserBulkException;
import com.revealprecision.revealserver.persistence.repository.OrganizationRepository;
import com.revealprecision.revealserver.persistence.repository.UserBulkExceptionRepository;
import com.revealprecision.revealserver.persistence.repository.UserBulkRepository;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import com.revealprecision.revealserver.service.KeycloakService;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@StepScope
@Component
public class UserItemProcessor implements ItemProcessor<UserBatchDTO, User> {

  private final UserBulkRepository userBulkRepository;
  private final UserBulkExceptionRepository userBulkExceptionRepository;
  private final UserRepository userRepository;
  private final OrganizationRepository organizationRepository;
  private final KeycloakService keycloakService;
  private final Keycloak keycloak;
  @Value("${keycloak.realm}")
  private String realm;
  private Set<String> alreadyAddedEmails = new HashSet<>();
  private Set<String> alreadyAddedUsernames = new HashSet<>();
  private List<String> listOfExistingUsernames;
  private List<String> listOfExistingEmails;
  private Map<UUID, Organization> organizations = new HashMap<>();
  private UserBulk userBulk;
  @Value("#{jobParameters['userBulkId']}")
  private String userBulkId;

  @BeforeStep
  private void setUpData() {
    userBulk = userBulkRepository.getById(UUID.fromString(userBulkId));
    listOfExistingEmails = userRepository.getAllEmails();
    listOfExistingUsernames = userRepository.getAllUsernames();
    organizations = organizationRepository.findAll().stream().collect(
        Collectors.toMap(organization -> organization.getIdentifier(),
            organization -> organization));
  }

  @Override
  public User process(UserBatchDTO item) throws Exception {
    if (!isUserValid(item)) {
      return null;
    }
    item.getSecurityGroups().remove("");
    User user = UserEntityFactory.toEntity(item);
    if (user.getEmail() != null) {
      alreadyAddedEmails.add(user.getEmail());
    }
    alreadyAddedUsernames.add(user.getUsername());
    setOrganizations(user, item.getOrganizations());
    user.setUserBulk(userBulk);
    if (createUserKeycloak(item, user) == 201) {
      user.setEntityStatus(EntityStatus.ACTIVE);
    }
    return user;
  }

  public boolean isUserValid(final UserBatchDTO userBatchDTO) {
    if (userBatchDTO.getUsername().isBlank()) {
      saveUserBulkException("Username can not be empty", userBatchDTO.getUsername());
      return false;
    }
    if (!userBatchDTO.getUsername().matches(BatchConstants.Constraint.USERNAME_REGEX)) {
      saveUserBulkException("Username can not contain special characters",
          userBatchDTO.getUsername());
      return false;
    } else if (userBatchDTO.getFirstName().isBlank() || userBatchDTO.getLastName().isBlank()
        || userBatchDTO.getPassword().isBlank()) {
      saveUserBulkException("Fist name, last name and password are mandatory",
          userBatchDTO.getUsername());
      return false;
    } else if (listOfExistingUsernames.contains(userBatchDTO.getUsername())
        || alreadyAddedUsernames.contains(userBatchDTO.getUsername())) {
      saveUserBulkException("User with username: " + userBatchDTO.getUsername() + " already exist",
          userBatchDTO.getUsername());
      return false;
    } else if (listOfExistingEmails.contains(userBatchDTO.getEmail())
        || alreadyAddedEmails.contains(userBatchDTO.getEmail())) {
      saveUserBulkException("User with email: " + userBatchDTO.getEmail() + " already exist",
          userBatchDTO.getUsername());
      return false;
    } else {
      return true;
    }
  }

  public void saveUserBulkException(String errorMessage, String username) {
    UserBulkException userBulkException = UserBulkException.builder()
        .userBulk(userBulk)
        .username(username)
        .message(errorMessage)
        .build();
    userBulkException.setEntityStatus(EntityStatus.ACTIVE);
    userBulkExceptionRepository.save(userBulkException);
  }

  public void setOrganizations(User user, Set<String> ids) {
    var uuids = ids.stream()
        .filter(s -> s.matches(Constraint.UUID_REGEX))
        .map(s -> UUID.fromString(s))
        .collect(Collectors.toSet());
    Set<Organization> userOrgs = new HashSet<>();
    uuids.forEach(id -> {
      if (organizations.containsKey(id)) {
        userOrgs.add(organizations.get(id));
      }
    });
    user.setOrganizations(userOrgs);
  }

  public int createUserKeycloak(UserBatchDTO userBatchDTO, User user) {
    UsersResource usersResource = keycloak.realm(realm).users();
    CredentialRepresentation credentialRepresentation = keycloakService.createPasswordCredentials(
        userBatchDTO.getPassword(), userBatchDTO.getTempPassword());

    UserRepresentation kcUser = new UserRepresentation();
    kcUser.setUsername(userBatchDTO.getUsername());
    kcUser.setCredentials(Collections.singletonList(credentialRepresentation));
    kcUser.setFirstName(userBatchDTO.getFirstName());
    kcUser.setLastName(userBatchDTO.getLastName());
    kcUser.setEmail(userBatchDTO.getEmail());
    kcUser.setEnabled(true);
    kcUser.setTotp(false);
    kcUser.setEmailVerified(false);
    kcUser.setNotBefore(0);
    kcUser.setAccess(KeycloakService.access);
    kcUser.setGroups(
        userBatchDTO.getSecurityGroups().stream().filter(group -> !group.isBlank()).collect(
            Collectors.toList()));
    Response response = usersResource.create(kcUser);
    String output = response.readEntity(String.class);
    response.close();

    if (response.getStatus() == 201) {
      UserRepresentation kcCreatedUser = usersResource.search(userBatchDTO.getUsername()).get(0);
      user.setSid(UUID.fromString(kcCreatedUser.getId()));
      user.setEntityStatus(EntityStatus.ACTIVE);
    } else if (response.getStatus() == 409) {
      JSONObject error = new JSONObject(output);
      user.setEntityStatus(EntityStatus.CREATING);
      user.setApiResponse(error.getString("errorMessage"));
    } else {
      user.setEntityStatus(EntityStatus.CREATING);
      user.setApiResponse("Unknown error on Keycloak");
    }
    return response.getStatus();
  }
}
