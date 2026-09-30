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

@Slf4j
@RequiredArgsConstructor
@Service
public class RasterIngestionService {

  private final IngestionTaskRepository ingestionTaskRepository;
  private final TileGenerator tileGenerator;
  private final IngestionTaskService ingestionTaskService;

  public void processIngestion(RasterIngestionMessage message) {
    try {
      // emitProgress(message, IngestionStage.STARTED, "Ingestion request received");
      // emitProgress(message, IngestionStage.VALIDATING, "Validating raster file and paths");

      tileGenerator.generateTiles(message);

      ingestionTaskService.stepCompleted(message.getRasterId());
    } catch (InvalidRasterEventException e) {
      handleFailure(message, e);
      throw e;
    } catch (Exception e) {
      handleFailure(message, e);
      throw new RasterProcessingException("Error during raster processing: " + e.getMessage(), e);
    }
  }

  private void handleFailure(RasterIngestionMessage message, Exception e) {
    log.error("Ingestion failed for taskIdentifier: {}. Error: {}", message.getRasterId(),
        e.getMessage());
    ingestionTaskService.stepFailed(message.getRasterId(), IngestionStage.FAILED, e.getMessage());
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
