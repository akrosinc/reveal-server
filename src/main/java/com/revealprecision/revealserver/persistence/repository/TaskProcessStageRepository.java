package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.enums.ProcessTrackerEnum;
import com.revealprecision.revealserver.persistence.domain.TaskProcessStage;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskProcessStageRepository extends EntityGraphJpaRepository<TaskProcessStage, UUID> {

  Optional<TaskProcessStage> findTaskGenerationStageByProcessTracker_IdentifierAndBaseEntityIdentifier(UUID processTrackerIdentifier, UUID baseEntityIdentifier);

  @Modifying
  @Query("update TaskProcessStage tgs set tgs.state = :newState where tgs.processTracker.identifier = :processTrackerIdentifier AND tgs.state = :state ")
  int updateTaskGenerationState(ProcessTrackerEnum newState, UUID processTrackerIdentifier, ProcessTrackerEnum state);

  int countByProcessTracker_IdentifierAndStateNot(UUID processTrackerIdentifier, ProcessTrackerEnum trackerEnum );

}
