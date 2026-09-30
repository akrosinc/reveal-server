package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.enums.IngestionStage;
import com.revealprecision.revealserver.persistence.domain.IngestionTask;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IngestionTaskRepository extends JpaRepository<IngestionTask, UUID> {
  Optional<IngestionTask> findByTaskIdentifier(String taskIdentifier);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE IngestionTask it SET it.completedSteps = it.completedSteps + 1, it.lastUpdated = :now WHERE it.taskIdentifier = :taskIdentifier")
  int incrementCompleted(@Param("taskIdentifier") String taskIdentifier, @Param("now") LocalDateTime now);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE IngestionTask it SET it.failed = true, it.stage = :stage, it.message = :message, it.lastUpdated = :now WHERE it.taskIdentifier = :taskIdentifier")
  int markFailed(@Param("taskIdentifier") String taskIdentifier, @Param("stage") IngestionStage stage, @Param("message") String message, @Param("now") LocalDateTime now);
}
