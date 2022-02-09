package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Target;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TargetRepository extends JpaRepository<Target, UUID> {

//  @Query(value = "select t from Target t where t.goal.identifier = :identifier")
//  Page<Target> getAll(@Param("identifier") String identifier, Pageable pageable);
}
