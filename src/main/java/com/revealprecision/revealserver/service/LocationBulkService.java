package com.revealprecision.revealserver.service;


import com.revealprecision.revealserver.enums.BulkStatusEnum;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.LocationBulk;
import com.revealprecision.revealserver.persistence.domain.UserBulk;
import com.revealprecision.revealserver.persistence.projection.LocationBulkProjection;
import com.revealprecision.revealserver.persistence.repository.LocationBulkExceptionRepository;
import com.revealprecision.revealserver.persistence.repository.LocationBulkRepository;
import com.revealprecision.revealserver.util.UserUtils;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class LocationBulkService {

  private final LocationBulkRepository locationBulkRepository;
  private final UserService userService;
  private final LocationBulkExceptionRepository locationBulkExceptionRepository;

  public UUID saveBulk(String file) {
    var locationBulk = LocationBulk.builder()
        .filename(file)
        .status(BulkStatusEnum.PROCESSING)
        .uploadedDatetime(LocalDateTime.now())
        .build();
    locationBulk.setEntityStatus(EntityStatus.ACTIVE);
    locationBulk.setUploadedBy(
        userService.getByKeycloakId(UUID.fromString(UserUtils.getCurrentPrinciple().getName()))
            .getUsername());
    locationBulk = locationBulkRepository.save(locationBulk);
    return locationBulk.getIdentifier();
  }

  @Transactional
  public LocationBulk findById(UUID identifier) {
    return locationBulkRepository.findById(identifier)
        .orElseThrow(
            () -> new NotFoundException(Pair.of(UserBulk.Fields.identifier, identifier),
                UserBulk.class));
  }

  public Page<LocationBulkProjection> getLocationBulkDetails(UUID identifier, Pageable pageable, String status) {
    switch (status){
      case "failed":
        return locationBulkExceptionRepository.getFailedLocationsFromBulk(identifier, pageable);
      case "successful":
        return locationBulkRepository.getSuccessfulLocationsFromBulk(identifier, pageable);
      default:
        return locationBulkRepository.findBulkById(identifier, pageable);
    }
  }

  public Page<LocationBulk> getLocationBulks(Pageable pageable) {
    return locationBulkRepository.findAll(pageable);
  }

  public boolean areRelationshipsGenerated() {
    return locationBulkRepository.countByStatusNot(BulkStatusEnum.COMPLETE) == 0;
  }

}
