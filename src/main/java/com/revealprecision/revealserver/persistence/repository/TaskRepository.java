package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Task;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    Optional<Task> findByIdentifier(UUID identifier);

    List<Task> findTaskByCode(String identifier);

//    Page<Task> findByPlanIdentifier(UUID planIdentifier, Integer pageNumber, Integer pageSize);
}