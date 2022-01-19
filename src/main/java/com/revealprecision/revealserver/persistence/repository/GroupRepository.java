package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Group;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

  Optional<Group> findByIdentifier(UUID identifier);

  Page<Group> findGroupByNameIgnoreCase(String name, Pageable pageable);

  Page<Group> findGroupByTypeIgnoreCase(String name, Pageable pageable);

  Page<Group> findGroupByNameIgnoreCaseAndTypeIgnoreCase(String name, String type,
      Pageable pageable);

  Page<Group> findGroupByNameContainingIgnoreCase(String name, Pageable pageable);
}