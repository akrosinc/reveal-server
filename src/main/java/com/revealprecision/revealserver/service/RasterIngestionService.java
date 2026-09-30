package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.enums.IngestionStage;
import com.revealprecision.revealserver.enums.IngestionType;
import com.revealprecision.revealserver.exceptions.InvalidRasterEventException;
import com.revealprecision.revealserver.exceptions.RasterProcessingException;
import com.revealprecision.revealserver.messaging.message.RasterIngestionMessage;
import com.revealprecision.revealserver.persistence.domain.IngestionTask;
import com.revealprecision.revealserver.persistence.repository.IngestionTaskRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class RasterIngestionService {

  private final IngestionTaskRepository ingestionTaskRepository;
  private final TileGenerator tileGenerator;


  public void processIngestion(RasterIngestionMessage message) {
    LocalDateTime startedAt = LocalDateTime.now();
    try {
//      emitProgress(message, IngestionStage.STARTED, "Ingestion request received");
//
//      emitProgress(message, IngestionStage.VALIDATING, "Validating raster file and paths");

      emitProgress(message, IngestionStage.PROCESSING, "Generating tiles");
      tileGenerator.generateTiles(
          message
      );

      emitProgress(message, IngestionStage.COMPLETED, "Tile generation completed successfully");
    } catch (InvalidRasterEventException e) {
      handleFailure(message, startedAt, e, IngestionStage.FAILED);
      throw e;
    } catch (Exception e) {
      handleFailure(message, startedAt, e, IngestionStage.FAILED);
      throw new RasterProcessingException("Error during raster processing: " + e.getMessage(), e);
    }
  }

  private void emitProgress(RasterIngestionMessage message, IngestionStage stage,
      String logMessage) {
    LocalDateTime now = LocalDateTime.now();
    updateIngestionStatus(message.getRasterId(), stage, logMessage, now);
  }

  private void handleFailure(RasterIngestionMessage message, LocalDateTime startedAt, Exception e,
      IngestionStage stage) {
    log.error("Ingestion failed for taskIdentifier: {}. Error: {}", message.getRasterId(),
        e.getMessage());
    emitProgress(message, stage, e.getMessage());
  }

  public void updateIngestionStatus(String rasterId, IngestionStage stage, String message,
      LocalDateTime timestamp) {
    IngestionTask status = ingestionTaskRepository.findByTaskIdentifier(rasterId)
        .orElse(IngestionTask.builder().taskIdentifier(rasterId).type(IngestionType.RASTER_IMPORT).build());

    status.setStage(stage);
    status.setMessage(message);
    status.setLastUpdated(timestamp);

    ingestionTaskRepository.save(status);
  }
}
