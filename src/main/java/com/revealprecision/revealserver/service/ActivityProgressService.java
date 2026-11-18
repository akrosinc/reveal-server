package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.facade.models.ActivityProgressDto;
import com.revealprecision.revealserver.persistence.domain.ActivityProgress;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.repository.ActivityProgressRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import com.revealprecision.revealserver.persistence.repository.PlanRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityProgressService {

  private final ActivityProgressRepository activityProgressRepository;
  private final PlanRepository planRepository;
  private final LocationRepository locationRepository;
  private final UserService userService;

  /**
   * Push: upsert by (plan, location). Last write wins (race handling between devices is not done
   * yet). A new server version is assigned only when the row is new or something changed, so an
   * identical re-push does not make every device re-pull the row. Rows that are invalid or point
   * to an unknown plan/location are skipped and logged, they do not fail the batch.
   *
   * @return the rows that were accepted, with their server versions
   */
  @Transactional
  public List<ActivityProgressDto> save(List<ActivityProgressDto> dtos) {
    List<ActivityProgressDto> saved = new ArrayList<>();
    if (dtos == null) {
      return saved;
    }

    for (ActivityProgressDto dto : dtos) {
      if (dto == null || dto.getPlanIdentifier() == null || dto.getLocationIdentifier() == null
          || dto.getState() == null) {
        log.warn("ACTIVITY_PROGRESS skipping invalid row (plan, location and state are required)");
        continue;
      }
      if (!planRepository.existsById(dto.getPlanIdentifier())
          || !locationRepository.existsById(dto.getLocationIdentifier())) {
        log.warn("ACTIVITY_PROGRESS skipping row for unknown plan {} or location {}",
            dto.getPlanIdentifier(), dto.getLocationIdentifier());
        continue;
      }

      ActivityProgress entity = activityProgressRepository
          .findByPlanIdentifierAndLocationIdentifier(dto.getPlanIdentifier(),
              dto.getLocationIdentifier())
          .orElseGet(() -> ActivityProgress.builder()
              .planIdentifier(dto.getPlanIdentifier())
              .locationIdentifier(dto.getLocationIdentifier())
              .build());

      boolean changed = entity.getIdentifier() == null
          || entity.getState() != dto.getState()
          || !Objects.equals(entity.getTotal(), zeroIfNull(dto.getTotal()))
          || !Objects.equals(entity.getNotVisited(), zeroIfNull(dto.getNotVisited()))
          || !Objects.equals(entity.getInProgress(), zeroIfNull(dto.getInProgress()))
          || !Objects.equals(entity.getComplete(), zeroIfNull(dto.getComplete()));

      if (changed) {
        entity.setState(dto.getState());
        entity.setTotal(zeroIfNull(dto.getTotal()));
        entity.setNotVisited(zeroIfNull(dto.getNotVisited()));
        entity.setInProgress(zeroIfNull(dto.getInProgress()));
        entity.setComplete(zeroIfNull(dto.getComplete()));
        entity.setServerVersion(activityProgressRepository.nextServerVersion());
        entity.setModifiedDatetime(LocalDateTime.now());
        entity = activityProgressRepository.save(entity);
      }
      saved.add(ActivityProgressDto.from(entity));
    }
    return saved;
  }

  /**
   * Pull: rows for the caller's selected plan assignments (including descendant locations of the
   * plan's own hierarchy), limited to the requested plans, with server version >= the cursor.
   */
  public List<ActivityProgressDto> sync(List<String> planIdentifiers, Long serverVersion) {
    List<UUID> planIds = planIdentifiers == null ? new ArrayList<>()
        : planIdentifiers.stream().map(UUID::fromString).collect(Collectors.toList());

    List<UUID> organizationIds = userService.getCurrentUser().getOrganizations().stream()
        .map(Organization::getIdentifier).collect(Collectors.toList());

    if (planIds.isEmpty() || organizationIds.isEmpty()) {
      return new ArrayList<>();
    }

    return activityProgressRepository
        .getForAssignedOrganizations(organizationIds, planIds,
            serverVersion == null ? 0L : serverVersion)
        .stream().map(ActivityProgressDto::from).collect(Collectors.toList());
  }

  private static Integer zeroIfNull(Integer value) {
    return value == null ? 0 : value;
  }
}
