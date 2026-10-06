package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.enums.BulkEntryStatus;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.MetadataImportType;
import com.revealprecision.revealserver.messaging.message.RasterLocationZonalStatsMessage;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.EntityTagOwnership;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.MetadataImport;
import com.revealprecision.revealserver.persistence.domain.MetadataImportOwnership;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.repository.EntityTagRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import com.revealprecision.revealserver.persistence.repository.MetadataImportRepository;
import com.revealprecision.revealserver.service.RasterLocationZonalStatsService;
import com.revealprecision.revealserver.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RasterLocationZonalStatsListener {

  private final RasterLocationZonalStatsService rasterLocationZonalStatsService;
  private final LocationRepository locationRepository;
  private final MetadataImportRepository metadataImportRepository;
  private final EntityTagRepository entityTagRepository;
  private final UserService userService;

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('RASTER_LOCATION_ZONAL_STATS')}",
      groupId = "reveal_server_group")
  public void calculateZonalStats(RasterLocationZonalStatsMessage message) {
    String rasterId = message.getRasterId();
    if (rasterId == null || rasterId.isBlank()) {
      log.error("Received zonal stats request with missing rasterId. Skipping and acknowledging.");
      return;
    }

    log.info("Received zonal stats request for rasterId: {}", rasterId);

    User user = userService.getByKeycloakId(message.getKeycloakId());

    MetadataImport metadataImport = MetadataImport.builder()
        .filename(rasterId)
        .metadataName(rasterId)
        .metadataImportType(MetadataImportType.RASTER)
        .status(BulkEntryStatus.BUSY)
        .uploadedBy(user != null ? user.getUsername() : null)
        .uploadedDatetime(LocalDateTime.now())
        .build();
    if (user != null) {
      MetadataImportOwnership metadataImportOwnership = MetadataImportOwnership.builder()
          .metadataImport(metadataImport)
          .userSid(user.getSid())
          .build();
      metadataImport.setOwners(List.of(metadataImportOwnership));
    }
    metadataImport.setEntityStatus(EntityStatus.ACTIVE);
    metadataImport = metadataImportRepository.save(metadataImport);

    if (message.getTagName() != null && !message.getTagName().isBlank()) {
      List<EntityTagOwnership> owners = null;
      if (metadataImport.getOwners() != null) {
        owners = metadataImport.getOwners().stream()
            .map(metadataImportOwnership -> EntityTagOwnership.builder()
                .userSid(metadataImportOwnership.getUserSid())
                .build())
            .collect(Collectors.toList());
      }

      String valueType = (message.getValueType() != null && !message.getValueType().isBlank())
          ? message.getValueType()
          : "double";

      EntityTag entityTag = EntityTag.builder()
          .tag(message.getTagName())
          .valueType(valueType)
          .definition(message.getTagName())
          .isPublic(false)
          .simulationDisplay(false)
          .isAggregate(false)
          .isDeleting(false)
          .metadataImport(metadataImport)
          .owners(owners)
          .build();
      if (entityTag.getOwners() != null) {
        entityTag.getOwners().forEach(owner -> owner.setEntityTag(entityTag));
      }
      entityTagRepository.save(entityTag);
    }

    try {
      List<Location> locations = (message.getSkipGeographicLevels() != null && !message.getSkipGeographicLevels().isEmpty())
          ? locationRepository.findLocationsInActiveHierarchyAndGeographicLevelNotIn(
              message.getSkipGeographicLevels().stream().map(String::toLowerCase).collect(Collectors.toList()))
          : locationRepository.findLocationsInActiveHierarchy();
      if (locations.isEmpty()) {
        log.warn("No locations found for zonal stats processing");
        return;
      }

      List<Location> toBeProcessedLocations = locations.stream()
          .filter(location -> !rasterLocationZonalStatsService
              .shouldSkipGeographicLevel(location, message.getSkipGeographicLevels()))
          .collect(Collectors.toList());

      rasterLocationZonalStatsService.calculateLocationZonalStats(
          rasterId, toBeProcessedLocations, message.isReprocess(), message.getTagName());

      metadataImport.setStatus(BulkEntryStatus.SUCCESSFUL);
      metadataImportRepository.save(metadataImport);

      log.info("Successfully calculated zonal stats for rasterId: {}", message.getRasterId());
    } catch (Exception e) {
      log.error("Unrecoverable error calculating zonal stats for rasterId {}: {}", rasterId, e.getMessage(), e);
      // Decide policy here: ack anyway to avoid a poison-pill message looping forever,
      // or skip ack to let it retry — but only if retries are actually likely to succeed.
    }
  }
}
