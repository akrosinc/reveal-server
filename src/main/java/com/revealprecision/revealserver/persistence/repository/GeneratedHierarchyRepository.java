package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.aggregation.GeneratedHierarchy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneratedHierarchyRepository extends JpaRepository<GeneratedHierarchy, Integer> {

  Optional<GeneratedHierarchy> findGeneratedHierarchyByName(String name);

}
