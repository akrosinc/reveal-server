package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.enums.BulkEntryStatus;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.MetadataImportType;
import com.revealprecision.revealserver.messaging.message.RasterLocationZonalStatsMessage;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.MetadataImport;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import com.revealprecision.revealserver.persistence.repository.MetadataImportRepository;
import com.revealprecision.revealserver.service.RasterLocationZonalStatsService;
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

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('RASTER_LOCATION_ZONAL_STATS')}",
      groupId = "reveal_server_group")
  public void calculateZonalStats(RasterLocationZonalStatsMessage message) {
    String rasterId = message.getRasterId();
    if (rasterId == null || rasterId.isBlank()) {
      log.error("Received zonal stats request with missing rasterId. Skipping and acknowledging.");
      return;
    }

    log.info("Received zonal stats request for rasterId: {}", rasterId);

    MetadataImport metadataImport = MetadataImport.builder()
        .filename(rasterId)
        .metadataName(rasterId)
        .metadataImportType(MetadataImportType.RASTER)
        .status(BulkEntryStatus.BUSY)
        .uploadedBy(message.getUploadedBy())
        .uploadedDatetime(LocalDateTime.now())
        .build();
    metadataImport.setEntityStatus(EntityStatus.ACTIVE);
    metadataImport = metadataImportRepository.save(metadataImport);

    try {
      List<Location> locations = locationRepository.findAll();
      if (locations.isEmpty()) {
        log.warn("No locations found for zonal stats processing");
        return;
      }

      List<Location> toBeProcessedLocations = locations.stream()
          .filter(location -> !rasterLocationZonalStatsService
              .shouldSkipGeographicLevel(location, message.getGeographicLevels()))
          .collect(Collectors.toList());

      rasterLocationZonalStatsService.calculateLocationZonalStats(
          rasterId, toBeProcessedLocations, message.isReprocess());

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
