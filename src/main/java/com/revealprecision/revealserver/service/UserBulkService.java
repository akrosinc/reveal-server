package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.enums.BulkStatusEnum;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.UserBulk;
import com.revealprecision.revealserver.persistence.projection.UserBulkProjection;
import com.revealprecision.revealserver.persistence.repository.UserBulkRepository;
import com.revealprecision.revealserver.util.UserUtils;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.keycloak.KeycloakPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserBulkService {

  private final UserBulkRepository userBulkRepository;
  private final UserService userService;

  public UUID saveBulk(String file) {

    KeycloakPrincipal principal = UserUtils.getKeyCloakPrincipal();

    UserBulk userBulk = new UserBulk();
    userBulk.setFilename(file);
    userBulk.setStatus(BulkStatusEnum.PROCESSING);
    userBulk.setEntityStatus(EntityStatus.ACTIVE);
    userBulk.setUploadedDatetime(LocalDateTime.now());
    userBulk.setUploadedBy(
        userService.getByKeycloakId(UUID.fromString(principal.getName())).getUsername());
    userBulk = userBulkRepository.save(userBulk);
    return userBulk.getIdentifier();
  }

  @Transactional
  public UserBulk findById(UUID identifier) {
    return userBulkRepository.findById(identifier)
        .orElseThrow(
            () -> new NotFoundException(Pair.of(UserBulk.Fields.identifier, identifier),
                UserBulk.class));
  }

  public Page<UserBulkProjection> getUserBulkDetails(UUID identifier, Pageable pageable) {
    return userBulkRepository.findBulkById(identifier, pageable);
  }

  public Page<UserBulk> getUserBulks(Pageable pageable) {
    return userBulkRepository.findAll(pageable);
  }
}
