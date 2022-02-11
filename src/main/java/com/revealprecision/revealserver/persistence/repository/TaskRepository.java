package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Task;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID>,
    JpaSpecificationExecutor<Task> {

  Optional<Task> findByIdentifier(UUID identifier);

  List<Task> findTasksByAction_IdentifierAndLocation_Identifier(UUID actionIdentifier,
      UUID locationIdentifier);

}