package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.enums.IngestionStage;
import com.revealprecision.revealserver.enums.IngestionType;
import com.revealprecision.revealserver.persistence.domain.IngestionTask;
import com.revealprecision.revealserver.persistence.repository.IngestionTaskRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class IngestionTaskService {

  private final IngestionTaskRepository ingestionTaskRepository;
  private final MapLayerService mapLayerService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public IngestionTask initIngestionTask(String rasterId, int totalSteps) {
    LocalDateTime now = LocalDateTime.now();
    IngestionTask task = ingestionTaskRepository.findByTaskIdentifier(rasterId)
        .orElseGet(() -> IngestionTask.builder()
            .taskIdentifier(rasterId)
            .type(IngestionType.RASTER_IMPORT)
            .build());

    task.setTotalSteps(totalSteps);
    task.setCompletedSteps(0);
    task.setFailed(false);
    task.setStage(IngestionStage.PROCESSING);
    task.setMessage("Generating tiles");
    task.setLastUpdated(now);

    return ingestionTaskRepository.saveAndFlush(task);
  }

  @Transactional
  public void stepCompleted(String rasterId) {
    LocalDateTime now = LocalDateTime.now();
    ingestionTaskRepository.incrementCompleted(rasterId, now);

    IngestionTask task = ingestionTaskRepository.findByTaskIdentifier(rasterId).orElse(null);
    if (task != null) {
      if (!task.isFailed() && task.getCompletedSteps() == task.getTotalSteps()) {
        task.setStage(IngestionStage.COMPLETED);
        task.setMessage("All zoom levels completed");
        task.setLastUpdated(LocalDateTime.now());
        ingestionTaskRepository.save(task);
        mapLayerService.activateMapLayer(rasterId);
        log.info("Ingestion completed for rasterId: {}. Activated map layer.", rasterId);
      } else {
        log.info("Progress for rasterId {}: {}/{} zoom levels done", rasterId, task.getCompletedSteps(), task.getTotalSteps());
      }
    }
  }

  @Transactional
  public void stepFailed(String rasterId, IngestionStage stage, String errorMessage) {
    LocalDateTime now = LocalDateTime.now();
    ingestionTaskRepository.markFailed(rasterId, stage, errorMessage, now);
    log.error("Ingestion failed for taskIdentifier: {}. Stage: {}, Error: {}", rasterId, stage, errorMessage);
  }
}
