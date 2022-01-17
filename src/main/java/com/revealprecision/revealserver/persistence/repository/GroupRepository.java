package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
     Optional<Group> findByIdentifier(UUID identifier);

     Page<Group> findGroupByNameIgnoreCase(String name, Pageable pageable);

     Page<Group> findGroupByNameAndLocation_NameIgnoreCase(String name, String locationName, Pageable pageable);

     Page<Group> findGroupByNameContainingIgnoreCaseOrLocation_NameContainingIgnoreCase(String name, String locationName, Pageable pageable);

     Page<Group> findGroupByLocation_NameIgnoreCase( String locationName, Pageable pageable);
}